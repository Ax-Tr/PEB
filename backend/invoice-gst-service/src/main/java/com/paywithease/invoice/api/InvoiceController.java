package com.paywithease.invoice.api;

import com.paywithease.invoice.application.EInvoicePayloadBuilder;
import com.paywithease.invoice.application.EwayPayloadBuilder;
import com.paywithease.invoice.application.InvoicePdfGenerator;
import com.paywithease.invoice.application.InvoiceService;
import com.paywithease.invoice.application.Readiness;
import com.paywithease.invoice.domain.GstTaxLine;
import com.paywithease.invoice.domain.Invoice;
import com.paywithease.invoice.domain.InvoiceItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Invoice, note, PDF and compliance-readiness API. */
@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "invoices", description = "GST invoices, notes, PDF and e-invoice/e-way readiness")
public class InvoiceController {

  private final InvoiceService service;
  private final InvoicePdfGenerator pdfGenerator;
  private final EInvoicePayloadBuilder eInvoiceBuilder;
  private final EwayPayloadBuilder ewayBuilder;

  public InvoiceController(
      InvoiceService service,
      InvoicePdfGenerator pdfGenerator,
      EInvoicePayloadBuilder eInvoiceBuilder,
      EwayPayloadBuilder ewayBuilder) {
    this.service = service;
    this.pdfGenerator = pdfGenerator;
    this.eInvoiceBuilder = eInvoiceBuilder;
    this.ewayBuilder = ewayBuilder;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a GST document (invoice / bill of supply / receipt voucher)")
  public InvoiceDtos.InvoiceResponse create(
      @Valid @RequestBody InvoiceDtos.CreateInvoiceRequest body) {
    Invoice invoice =
        service.create(
            new InvoiceService.CreateCommand(
                body.documentType(),
                body.supplyType(),
                body.customerId(),
                body.customerName(),
                body.customerGstin(),
                body.placeOfSupply(),
                body.businessStateCode(),
                body.reverseCharge(),
                body.invoiceDate(),
                toLineCommands(body.lines()),
                null,
                null));
    return toResponse(invoice);
  }

  @GetMapping
  @Operation(summary = "List invoices for the current tenant, newest first")
  public List<InvoiceDtos.InvoiceResponse> list() {
    return service.list().stream().map(this::toResponse).toList();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get an invoice with items and tax summary")
  public InvoiceDtos.InvoiceResponse get(@PathVariable String id) {
    return toResponse(service.get(id));
  }

  @GetMapping("/{id}/pdf")
  @Operation(summary = "Render an invoice/note as a PDF")
  public ResponseEntity<byte[]> pdf(@PathVariable String id) {
    Invoice invoice = service.get(id);
    List<InvoiceItem> items = service.items(id);
    List<GstTaxLine> taxLines = service.taxLines(id);
    byte[] bytes = pdfGenerator.generate(invoice, items, taxLines);
    String filename = safeFileName(invoice.getInvoiceNumber()) + ".pdf";
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
        .body(bytes);
  }

  @PostMapping("/{id}/send")
  @Operation(summary = "Mark an invoice as sent over a channel")
  public InvoiceDtos.InvoiceResponse send(
      @PathVariable String id, @RequestBody(required = false) InvoiceDtos.SendRequest body) {
    String channel = body == null ? null : body.channel();
    service.markSent(id, channel);
    return toResponse(service.get(id));
  }

  @GetMapping("/{id}/e-invoice-payload")
  @Operation(summary = "Build the IRP e-invoice payload (readiness only)")
  public InvoiceDtos.EInvoiceReadinessResponse eInvoicePayload(@PathVariable String id) {
    Invoice invoice = service.get(id);
    Readiness readiness = eInvoiceBuilder.build(invoice, service.items(id));
    return toReadiness(readiness);
  }

  @GetMapping("/{id}/eway-payload")
  @Operation(summary = "Build the e-way-bill payload (readiness only)")
  public InvoiceDtos.EInvoiceReadinessResponse ewayPayload(@PathVariable String id) {
    Invoice invoice = service.get(id);
    Readiness readiness = ewayBuilder.build(invoice, service.items(id));
    return toReadiness(readiness);
  }

  @PostMapping("/credit-notes")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a credit note against an original document")
  public InvoiceDtos.InvoiceResponse createCreditNote(
      @Valid @RequestBody InvoiceDtos.CreateNoteRequest body) {
    return toResponse(service.createCreditNote(toNoteCommand(body)));
  }

  @PostMapping("/debit-notes")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a debit note against an original document")
  public InvoiceDtos.InvoiceResponse createDebitNote(
      @Valid @RequestBody InvoiceDtos.CreateNoteRequest body) {
    return toResponse(service.createDebitNote(toNoteCommand(body)));
  }

  private InvoiceService.CreateCommand toNoteCommand(InvoiceDtos.CreateNoteRequest body) {
    return new InvoiceService.CreateCommand(
        body.documentType(),
        body.supplyType(),
        body.customerId(),
        body.customerName(),
        body.customerGstin(),
        body.placeOfSupply(),
        body.businessStateCode(),
        body.reverseCharge(),
        body.invoiceDate(),
        toLineCommands(body.lines()),
        body.originalDocumentId(),
        body.reason());
  }

  private static List<InvoiceService.LineCommand> toLineCommands(List<InvoiceDtos.LineDto> lines) {
    return lines.stream()
        .map(
            l ->
                new InvoiceService.LineCommand(
                    l.productId(),
                    l.description(),
                    l.hsnSac(),
                    l.quantity(),
                    l.unitPriceMinor(),
                    l.discountMinor(),
                    l.gstRate()))
        .toList();
  }

  private InvoiceDtos.InvoiceResponse toResponse(Invoice inv) {
    List<InvoiceDtos.ItemDto> itemDtos =
        service.items(inv.getId()).stream()
            .map(
                i ->
                    new InvoiceDtos.ItemDto(
                        i.getProductId(),
                        i.getDescription(),
                        i.getHsnSac(),
                        i.getQuantity(),
                        i.getUnitPriceMinor(),
                        i.getDiscountMinor(),
                        i.getGstRate(),
                        i.getTaxableValueMinor(),
                        i.getCgstMinor(),
                        i.getSgstMinor(),
                        i.getIgstMinor(),
                        i.getLineTotalMinor()))
            .toList();
    List<InvoiceDtos.TaxLineDto> taxLineDtos =
        service.taxLines(inv.getId()).stream()
            .map(
                t ->
                    new InvoiceDtos.TaxLineDto(
                        t.getGstRate(),
                        t.getTaxableValueMinor(),
                        t.getCgstMinor(),
                        t.getSgstMinor(),
                        t.getIgstMinor()))
            .toList();
    return new InvoiceDtos.InvoiceResponse(
        inv.getId(),
        inv.getDocumentType(),
        inv.getSupplyType(),
        inv.getInvoiceNumber(),
        inv.getFinancialYear(),
        inv.getInvoiceDate(),
        inv.getCustomerName(),
        inv.getCustomerGstin(),
        inv.getPlaceOfSupply(),
        inv.isReverseCharge(),
        inv.isTaxable(),
        inv.getTotalTaxableMinor(),
        inv.getTotalCgstMinor(),
        inv.getTotalSgstMinor(),
        inv.getTotalIgstMinor(),
        inv.getTotalTaxMinor(),
        inv.getTotalAmountMinor(),
        inv.getStatus(),
        itemDtos,
        taxLineDtos);
  }

  private static InvoiceDtos.EInvoiceReadinessResponse toReadiness(Readiness readiness) {
    return new InvoiceDtos.EInvoiceReadinessResponse(
        readiness.ready(), readiness.missingFields(), readiness.payload());
  }

  private static String safeFileName(String invoiceNumber) {
    return invoiceNumber == null ? "invoice" : invoiceNumber.replace('/', '_');
  }
}
