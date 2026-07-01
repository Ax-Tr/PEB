package com.paywithease.product.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Request/response DTOs for the /products and /hsn-sac APIs. */
public final class ProductDtos {

  private ProductDtos() {}

  public record CreateProduct(
      @NotBlank String name,
      @NotBlank String type,
      @NotBlank String hsnSac,
      @NotNull BigDecimal gstRate,
      @NotBlank String unit,
      @PositiveOrZero long salePriceMinor,
      @PositiveOrZero long purchasePriceMinor,
      BigDecimal marginDefault) {}

  public record ProductResponse(
      String id,
      String name,
      String type,
      String hsnSac,
      BigDecimal gstRate,
      String unit,
      long salePriceMinor,
      long purchasePriceMinor,
      BigDecimal marginDefault) {}

  public record HsnSacResponse(String code, String description, BigDecimal gstRate, String kind) {}
}
