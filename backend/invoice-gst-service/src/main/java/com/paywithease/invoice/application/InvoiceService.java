package com.paywithease.invoice.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.gst.GstCalculator;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.invoice.domain.DocumentType;
import com.paywithease.invoice.domain.GstTaxLine;
import com.paywithease.invoice.domain.Invoice;
import com.paywithease.invoice.domain.InvoiceItem;
import com.paywithease.invoice.domain.SupplyType;
import com.paywithease.invoice.infrastructure.GstTaxLineRepository;
import com.paywithease.invoice.infrastructure.InvoiceItemRepository;
import com.paywithease.invoice.infrastructure.InvoiceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Invoice/note issuance: computes GST via {@link GstCalculator}, allocates a document number,
 * persists the invoice with its items and per-rate tax summary, and emits INVOICE_GENERATED /
 * INVOICE_SENT events. All amounts are integer paise.
 */
@Service
public class InvoiceService {

  private static final String SOURCE = "invoice-gst-service";
  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

  private final InvoiceRepository invoices;
  private final InvoiceItemRepository items;
  private final GstTaxLineRepository taxLines;
  private final InvoiceNumberService numberService;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  private final String taxInvoicePrefix;
  private final String billOfSupplyPrefix;
  private final String receiptVoucherPrefix;
  private final String creditNotePrefix;
  private final String debitNotePrefix;
  private final String defaultStateCode;

  public InvoiceService(
      InvoiceRepository invoices,
      InvoiceItemRepository items,
      GstTaxLineRepository taxLines,
      InvoiceNumberService numberService,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock,
      @Value("${peb.invoice.prefixes.tax-invoice:INV}") String taxInvoicePrefix,
      @Value("${peb.invoice.prefixes.bill-of-supply:BOS}") String billOfSupplyPrefix,
      @Value("${peb.invoice.prefixes.receipt-voucher:RCV}") String receiptVoucherPrefix,
      @Value("${peb.invoice.prefixes.credit-note:CRN}") String creditNotePrefix,
      @Value("${peb.invoice.prefixes.debit-note:DBN}") String debitNotePrefix,
      @Value("${peb.invoice.default-state-code:27}") String defaultStateCode) {
    this.invoices = invoices;
    this.items = items;
    this.taxLines = taxLines;
    this.numberService = numberService;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.taxInvoicePrefix = taxInvoicePrefix;
    this.billOfSupplyPrefix = billOfSupplyPrefix;
    this.receiptVoucherPrefix = receiptVoucherPrefix;
    this.creditNotePrefix = creditNotePrefix;
    this.debitNotePrefix = debitNotePrefix;
    this.defaultStateCode = defaultStateCode;
  }

  public record LineCommand(
      String productId,
      String description,
      String hsnSac,
      BigDecimal quantity,
      long unitPriceMinor,
      long discountMinor,
      BigDecimal gstRate) {}

  public record CreateCommand(
      String documentType,
      String supplyType,
      String customerId,
      String customerName,
      String customerGstin,
      String placeOfSupply,
      String businessStateCode,
      boolean reverseCharge,
      LocalDate invoiceDate,
      List<LineCommand> lines,
      String originalDocumentId,
      String reason) {}

  @Transactional
  public Invoice create(CreateCommand cmd) {
    String tenantId = TenantContext.requireTenantId();

    if (!DocumentType.isValid(cmd.documentType())) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED, "Unknown documentType: " + cmd.documentType());
    }
    if (!SupplyType.isValid(cmd.supplyType())) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED, "Unknown supplyType: " + cmd.supplyType());
    }
    DocumentType docType = DocumentType.valueOf(cmd.documentType());
    if (docType.isNote() && isBlank(cmd.originalDocumentId())) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED, "originalDocumentId is required for notes");
    }
    if (cmd.lines() == null || cmd.lines().isEmpty()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "at least one line is required");
    }

    boolean taxable = docType != DocumentType.BILL_OF_SUPPLY;
    String businessState =
        isBlank(cmd.businessStateCode()) ? defaultStateCode : cmd.businessStateCode();
    String placeOfSupply = resolvePlaceOfSupply(cmd, businessState);
    LocalDate date =
        cmd.invoiceDate() != null ? cmd.invoiceDate() : LocalDate.ofInstant(clock.instant(), IST);

    // Compute per-line taxable value (paise) and gather GST inputs.
    List<Long> taxableValues = new ArrayList<>();
    List<GstCalculator.LineInput> inputs = new ArrayList<>();
    for (LineCommand line : cmd.lines()) {
      long taxableValue = taxableValueMinor(line);
      taxableValues.add(taxableValue);
      BigDecimal rate = line.gstRate() == null ? BigDecimal.ZERO : line.gstRate();
      inputs.add(new GstCalculator.LineInput(taxableValue, rate));
    }

    GstCalculator.Result result =
        GstCalculator.compute(businessState, placeOfSupply, inputs, taxable, cmd.reverseCharge());

    String prefix = prefixFor(docType);
    String number = numberService.next(tenantId, docType, prefix, date);
    String fy = numberService.financialYear(date);

    Instant now = clock.instant();
    String invoiceId = Ulid.newId();
    Invoice invoice =
        new Invoice(
            invoiceId,
            tenantId,
            docType.name(),
            cmd.supplyType(),
            cmd.customerId(),
            cmd.customerName(),
            cmd.customerGstin(),
            placeOfSupply,
            businessState,
            number,
            fy,
            date,
            cmd.originalDocumentId(),
            cmd.reason(),
            cmd.reverseCharge(),
            taxable,
            result.totalTaxableMinor(),
            result.totalCgstMinor(),
            result.totalSgstMinor(),
            result.totalIgstMinor(),
            result.totalTaxMinor(),
            result.documentTotalMinor(),
            now);
    invoices.save(invoice);

    for (int i = 0; i < cmd.lines().size(); i++) {
      LineCommand line = cmd.lines().get(i);
      long taxableValue = taxableValues.get(i);
      GstCalculator.LineTax lineTax = result.lines().get(i);
      long lineTotal = cmd.reverseCharge() ? taxableValue : taxableValue + lineTax.totalTaxMinor();
      items.save(
          new InvoiceItem(
              Ulid.newId(),
              tenantId,
              invoiceId,
              line.productId(),
              line.description(),
              line.hsnSac(),
              line.quantity(),
              line.unitPriceMinor(),
              line.discountMinor(),
              line.gstRate() == null ? BigDecimal.ZERO : line.gstRate(),
              taxableValue,
              lineTax.cgstMinor(),
              lineTax.sgstMinor(),
              lineTax.igstMinor(),
              lineTotal));
    }

    for (GstCalculator.RateSummary rate : result.summaryByRate()) {
      taxLines.save(
          new GstTaxLine(
              Ulid.newId(),
              tenantId,
              invoiceId,
              rate.gstRatePercent(),
              rate.taxableValueMinor(),
              rate.cgstMinor(),
              rate.sgstMinor(),
              rate.igstMinor()));
    }

    audit.record(
        "INVOICE_GENERATED",
        "invoice",
        invoiceId,
        Map.of(
            "number", number,
            "docType", docType.name(),
            "totalAmountMinor", invoice.getTotalAmountMinor()));

    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("invoiceId", invoiceId);
    payload.put("invoiceNumber", number);
    payload.put("customerId", cmd.customerId());
    payload.put("totalAmountMinor", invoice.getTotalAmountMinor());
    payload.put("totalTaxMinor", invoice.getTotalTaxMinor());
    payload.put("supplyType", cmd.supplyType());
    emit("INVOICE_GENERATED", invoice, payload);

    return invoice;
  }

  @Transactional
  public Invoice createCreditNote(CreateCommand cmd) {
    return create(withType(cmd, DocumentType.CREDIT_NOTE));
  }

  @Transactional
  public Invoice createDebitNote(CreateCommand cmd) {
    return create(withType(cmd, DocumentType.DEBIT_NOTE));
  }

  @Transactional(readOnly = true)
  public Invoice get(String id) {
    return invoices
        .findByTenantIdAndId(TenantContext.requireTenantId(), id)
        .orElseThrow(() -> ApiException.notFound("Invoice"));
  }

  @Transactional(readOnly = true)
  public List<InvoiceItem> items(String invoiceId) {
    return items.findByInvoiceId(invoiceId);
  }

  @Transactional(readOnly = true)
  public List<GstTaxLine> taxLines(String invoiceId) {
    return taxLines.findByInvoiceId(invoiceId);
  }

  @Transactional
  public Invoice markSent(String id, String channel) {
    Invoice invoice = get(id);
    invoice.markSent(clock.instant());
    invoices.save(invoice);
    audit.record("INVOICE_SENT", "invoice", id, Map.of("channel", nullSafe(channel)));
    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("invoiceId", id);
    payload.put("invoiceNumber", invoice.getInvoiceNumber());
    payload.put("channel", nullSafe(channel));
    emit("INVOICE_SENT", invoice, payload);
    return invoice;
  }

  @Transactional(readOnly = true)
  public List<Invoice> salesRegister(LocalDate from, LocalDate to) {
    return invoices.findByTenantIdAndInvoiceDateBetweenOrderByInvoiceDateAsc(
        TenantContext.requireTenantId(), from, to);
  }

  private String resolvePlaceOfSupply(CreateCommand cmd, String businessState) {
    if (!isBlank(cmd.placeOfSupply())) {
      return cmd.placeOfSupply();
    }
    if (cmd.customerGstin() != null && cmd.customerGstin().length() >= 2) {
      return cmd.customerGstin().substring(0, 2);
    }
    return businessState;
  }

  private long taxableValueMinor(LineCommand line) {
    BigDecimal quantity = line.quantity() == null ? BigDecimal.ONE : line.quantity();
    long gross =
        quantity
            .multiply(BigDecimal.valueOf(line.unitPriceMinor()))
            .setScale(0, RoundingMode.HALF_EVEN)
            .longValueExact();
    long net = gross - line.discountMinor();
    if (net < 0) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "discount exceeds line value");
    }
    return net;
  }

  private String prefixFor(DocumentType docType) {
    return switch (docType) {
      case TAX_INVOICE -> taxInvoicePrefix;
      case BILL_OF_SUPPLY -> billOfSupplyPrefix;
      case RECEIPT_VOUCHER -> receiptVoucherPrefix;
      case CREDIT_NOTE -> creditNotePrefix;
      case DEBIT_NOTE -> debitNotePrefix;
    };
  }

  private static CreateCommand withType(CreateCommand cmd, DocumentType docType) {
    return new CreateCommand(
        docType.name(),
        cmd.supplyType(),
        cmd.customerId(),
        cmd.customerName(),
        cmd.customerGstin(),
        cmd.placeOfSupply(),
        cmd.businessStateCode(),
        cmd.reverseCharge(),
        cmd.invoiceDate(),
        cmd.lines(),
        cmd.originalDocumentId(),
        cmd.reason());
  }

  private void emit(String eventType, Invoice invoice, ObjectNode payload) {
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(invoice.getTenantId())
            .businessId(invoice.getTenantId())
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(invoice.getId())
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static String nullSafe(String s) {
    return s == null ? "" : s;
  }
}
