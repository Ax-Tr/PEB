package com.paywithease.employee.domain.payroll;

import java.math.BigDecimal;

/**
 * Statutory payroll rates. These are development defaults and MUST be verified against the CURRENT
 * official rules (EPFO for PF, ESIC for ESI, the relevant State's Professional Tax schedule) and a
 * qualified CA before production payroll — the system never fabricates compliance rules. In
 * production these are sourced from the Rules Engine (state/industry-scoped), not hard-coded.
 *
 * <p>All monetary values are integer paise; rates are percentages.
 */
public record PayrollRates(
    BigDecimal pfRatePercent,
    long pfWageCeilingMinor,
    BigDecimal esiRatePercent,
    long esiGrossThresholdMinor,
    long ptFlatMinor) {

  /**
   * Indicative defaults (verify before use): PF 12% capped at ₹15,000 basic; ESI 0.75% if gross ≤
   * ₹21,000; PT ₹200 flat.
   */
  public static PayrollRates defaults() {
    return new PayrollRates(
        new BigDecimal("12"),
        1_500_000L, // ₹15,000 PF wage ceiling
        new BigDecimal("0.75"),
        2_100_000L, // ₹21,000 ESI gross threshold
        20_000L); // ₹200 professional tax
  }
}
