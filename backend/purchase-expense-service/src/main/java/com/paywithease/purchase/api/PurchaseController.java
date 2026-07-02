package com.paywithease.purchase.api;

import com.paywithease.purchase.application.PurchaseService;
import com.paywithease.purchase.domain.PurchaseBill;
import com.paywithease.purchase.domain.PurchaseItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Purchase-bill recording API with input GST (ITC) computation. */
@RestController
@RequestMapping("/api/v1/purchase-bills")
@Tag(name = "purchase-bills", description = "Vendor purchase bills and input GST (ITC)")
public class PurchaseController {

  private static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);
  private static final LocalDate MAX_DATE = LocalDate.of(2999, 1, 1);

  private final PurchaseService service;

  public PurchaseController(PurchaseService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record a vendor purchase bill and compute input GST")
  public PurchaseDtos.PurchaseBillResponse create(
      @Valid @RequestBody PurchaseDtos.CreateBillRequest body) {
    PurchaseBill bill =
        service.createBill(
            new PurchaseService.CreateBillCommand(
                body.vendorId(),
                body.vendorName(),
                body.vendorGstin(),
                body.billNumber(),
                body.placeOfSupply(),
                body.businessStateCode(),
                body.reverseCharge(),
                body.billDate(),
                toLineCommands(body.lines())));
    return toResponse(bill);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a purchase bill with its items")
  public PurchaseDtos.PurchaseBillResponse get(@PathVariable String id) {
    return toResponse(service.getBill(id));
  }

  @GetMapping
  @Operation(summary = "List purchase bills, optionally within a date range")
  public List<PurchaseDtos.PurchaseBillResponse> list(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    LocalDate f = from != null ? from : MIN_DATE;
    LocalDate t = to != null ? to : MAX_DATE;
    return service.register(f, t).stream().map(this::toResponse).toList();
  }

  private static List<PurchaseService.LineCommand> toLineCommands(
      List<PurchaseDtos.LineDto> lines) {
    return lines.stream()
        .map(
            l ->
                new PurchaseService.LineCommand(
                    l.productId(),
                    l.description(),
                    l.hsnSac(),
                    l.quantity(),
                    l.unitPriceMinor(),
                    l.discountMinor(),
                    l.gstRate()))
        .toList();
  }

  private PurchaseDtos.PurchaseBillResponse toResponse(PurchaseBill bill) {
    List<PurchaseDtos.ItemDto> itemDtos =
        service.items(bill.getId()).stream().map(PurchaseController::toItemDto).toList();
    return new PurchaseDtos.PurchaseBillResponse(
        bill.getId(),
        bill.getVendorId(),
        bill.getVendorName(),
        bill.getBillNumber(),
        bill.getBillDate(),
        bill.getPlaceOfSupply(),
        bill.isReverseCharge(),
        bill.getTotalTaxableMinor(),
        bill.getTotalInputGstMinor(),
        bill.getTotalCgstMinor(),
        bill.getTotalSgstMinor(),
        bill.getTotalIgstMinor(),
        bill.getTotalAmountMinor(),
        bill.getStatus(),
        itemDtos);
  }

  private static PurchaseDtos.ItemDto toItemDto(PurchaseItem i) {
    return new PurchaseDtos.ItemDto(
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
        i.getLineTotalMinor());
  }
}
