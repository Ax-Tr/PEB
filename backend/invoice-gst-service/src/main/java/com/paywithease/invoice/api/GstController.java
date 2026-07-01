package com.paywithease.invoice.api;

import com.paywithease.invoice.application.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** GST reporting API (sales register / GSTR feed). */
@RestController
@RequestMapping("/api/v1/gst")
@Tag(name = "gst", description = "GST reports (sales register)")
public class GstController {

  private final InvoiceService service;

  public GstController(InvoiceService service) {
    this.service = service;
  }

  @GetMapping("/sales-register")
  @Operation(summary = "Sales register for a date range (inclusive)")
  public List<InvoiceDtos.SalesRegisterRow> salesRegister(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return service.salesRegister(from, to).stream()
        .map(
            inv ->
                new InvoiceDtos.SalesRegisterRow(
                    inv.getId(),
                    inv.getInvoiceNumber(),
                    inv.getInvoiceDate(),
                    inv.getCustomerName(),
                    inv.getTotalTaxableMinor(),
                    inv.getTotalTaxMinor(),
                    inv.getTotalAmountMinor()))
        .toList();
  }
}
