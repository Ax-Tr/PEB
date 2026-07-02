package com.paywithease.purchase.api;

import com.paywithease.purchase.application.PurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Purchase register: input-GST summary rows over a date range (GSTR / ITC reporting). */
@RestController
@RequestMapping("/api/v1/purchase-register")
@Tag(name = "purchase-register", description = "Purchase register (input GST / ITC) over a period")
public class PurchaseRegisterController {

  private final PurchaseService service;

  public PurchaseRegisterController(PurchaseService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "Purchase register rows between two dates")
  public List<PurchaseDtos.PurchaseRegisterRow> register(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return service.register(from, to).stream()
        .map(
            b ->
                new PurchaseDtos.PurchaseRegisterRow(
                    b.getId(),
                    b.getBillNumber(),
                    b.getBillDate(),
                    b.getVendorName(),
                    b.getTotalTaxableMinor(),
                    b.getTotalInputGstMinor(),
                    b.getTotalAmountMinor()))
        .toList();
  }
}
