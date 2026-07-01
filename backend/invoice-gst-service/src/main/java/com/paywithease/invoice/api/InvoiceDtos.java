package com.paywithease.invoice.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Request/response DTOs for the /invoices and /gst APIs. */
public final class InvoiceDtos {

  private InvoiceDtos() {}

  public record LineDto(
      String productId,
      @NotBlank String description,
      String hsnSac,
      @NotNull @Positive BigDecimal quantity,
      @PositiveOrZero long unitPriceMinor,
      @PositiveOrZero long discountMinor,
      @NotNull BigDecimal gstRate) {}

  public record CreateInvoiceRequest(
      @NotBlank String documentType,
      @NotBlank String supplyType,
      String customerId,
      String customerName,
      String customerGstin,
      String placeOfSupply,
      String businessStateCode,
      boolean reverseCharge,
      LocalDate invoiceDate,
      @NotEmpty List<LineDto> lines) {}

  public record CreateNoteRequest(
      @NotBlank String documentType,
      @NotBlank String supplyType,
      String customerId,
      String customerName,
      String customerGstin,
      String placeOfSupply,
      String businessStateCode,
      boolean reverseCharge,
      LocalDate invoiceDate,
      @NotEmpty List<LineDto> lines,
      @NotBlank String originalDocumentId,
      String reason) {}

  public record ItemDto(
      String productId,
      String description,
      String hsnSac,
      BigDecimal quantity,
      long unitPriceMinor,
      long discountMinor,
      BigDecimal gstRate,
      long taxableValueMinor,
      long cgstMinor,
      long sgstMinor,
      long igstMinor,
      long lineTotalMinor) {}

  public record TaxLineDto(
      BigDecimal gstRate, long taxableValueMinor, long cgstMinor, long sgstMinor, long igstMinor) {}

  public record InvoiceResponse(
      String id,
      String documentType,
      String supplyType,
      String invoiceNumber,
      String financialYear,
      LocalDate invoiceDate,
      String customerName,
      String customerGstin,
      String placeOfSupply,
      boolean reverseCharge,
      boolean taxable,
      long totalTaxableMinor,
      long totalCgstMinor,
      long totalSgstMinor,
      long totalIgstMinor,
      long totalTaxMinor,
      long totalAmountMinor,
      String status,
      List<ItemDto> items,
      List<TaxLineDto> taxLines) {}

  public record SendRequest(String channel) {}

  public record EInvoiceReadinessResponse(
      boolean ready, List<String> missingFields, Object payload) {}

  public record SalesRegisterRow(
      String id,
      String invoiceNumber,
      LocalDate invoiceDate,
      String customerName,
      long totalTaxableMinor,
      long totalTaxMinor,
      long totalAmountMinor) {}
}
