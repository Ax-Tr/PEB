package com.paywithease.product.domain;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Validator for the GST rates permitted on a product/service line. India levies GST at a fixed set
 * of slabs; anything outside the set is rejected at catalog-entry time. Rates are compared by
 * numeric value (scale-insensitive) so {@code 18}, {@code 18.0} and {@code 18.00} are equivalent.
 */
public final class GstRate {

  private static final Set<BigDecimal> ALLOWED =
      Set.of(
          new BigDecimal("0"),
          new BigDecimal("0.25"),
          new BigDecimal("3"),
          new BigDecimal("5"),
          new BigDecimal("12"),
          new BigDecimal("18"),
          new BigDecimal("28"));

  private GstRate() {}

  /**
   * True when {@code rate} equals one of the permitted GST slabs (ignoring trailing-zero scale).
   */
  public static boolean isAllowed(BigDecimal rate) {
    if (rate == null) {
      return false;
    }
    for (BigDecimal allowed : ALLOWED) {
      if (allowed.compareTo(rate) == 0) {
        return true;
      }
    }
    return false;
  }

  /** Parse and validate a GST rate string, returning the canonical BigDecimal. */
  public static BigDecimal of(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("GST rate is required");
    }
    BigDecimal rate = new BigDecimal(value.trim());
    if (!isAllowed(rate)) {
      throw new IllegalArgumentException("Unsupported GST rate: " + value);
    }
    return rate;
  }
}
