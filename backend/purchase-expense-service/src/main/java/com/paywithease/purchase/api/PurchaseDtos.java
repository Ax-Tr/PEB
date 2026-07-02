package com.paywithease.purchase.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Request/response DTOs for the purchase-bill and expense APIs. */
public final class PurchaseDtos {

  private PurchaseDtos() {}

  public record LineDto(
      String productId,
      @NotBlank String description,
      String hsnSac,
      @NotNull @Positive BigDecimal quantity,
      @PositiveOrZero long unitPriceMinor,
      @PositiveOrZero long discountMinor,
      @NotNull BigDecimal gstRate) {}

  public record CreateBillRequest(
      String vendorId,
      String vendorName,
      String vendorGstin,
      String billNumber,
      String placeOfSupply,
      String businessStateCode,
      boolean reverseCharge,
      LocalDate billDate,
      @NotEmpty List<LineDto> lines) {}

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

  public record PurchaseBillResponse(
      String id,
      String vendorId,
      String vendorName,
      String billNumber,
      LocalDate billDate,
      String placeOfSupply,
      boolean reverseCharge,
      long totalTaxableMinor,
      long totalInputGstMinor,
      long totalCgstMinor,
      long totalSgstMinor,
      long totalIgstMinor,
      long totalAmountMinor,
      String status,
      List<ItemDto> items) {}

  public record CreateExpenseRequest(
      @NotBlank String category,
      String description,
      @Positive long amountMinor,
      BigDecimal gstRate,
      String vendorId,
      LocalDate expenseDate) {}

  public record ExpenseResponse(
      String id,
      String category,
      String description,
      long amountMinor,
      BigDecimal gstRate,
      long inputGstMinor,
      String vendorId,
      LocalDate expenseDate,
      String status,
      String approvedBy) {}

  public record PurchaseRegisterRow(
      String id,
      String billNumber,
      LocalDate billDate,
      String vendorName,
      long totalTaxableMinor,
      long totalInputGstMinor,
      long totalAmountMinor) {}
}
