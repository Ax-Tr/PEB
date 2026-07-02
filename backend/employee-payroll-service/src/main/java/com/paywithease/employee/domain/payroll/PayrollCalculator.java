package com.paywithease.employee.domain.payroll;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.money.Money;

/**
 * Pure monthly payroll calculation engine (no framework dependencies) — the correctness core of the
 * payroll run. Computes LOP-prorated earnings, statutory deductions (PF/ESI/PT), and net pay from a
 * salary structure. Rates come from {@link PayrollRates} (Rules Engine / config, CA-verified).
 *
 * <p>Model (all amounts integer paise):
 *
 * <ul>
 *   <li>LOP deduction = round(gross × lopDays / workingDays); earned gross = gross − LOP.
 *   <li>PF = pfRate% of min(earned basic, PF wage ceiling), when PF applies.
 *   <li>ESI = esiRate% of earned gross, only when ESI applies and gross ≤ ESI threshold.
 *   <li>PT = flat amount when PT applies.
 *   <li>TDS is supplied per run (full income-tax-slab computation is a later refinement).
 *   <li>Net = earned gross + incentives − PF − ESI − PT − TDS − other deductions.
 * </ul>
 */
public final class PayrollCalculator {

  private PayrollCalculator() {}

  public record Input(
      long grossMinor,
      long basicMinor,
      int workingDays,
      int lopDays,
      long incentivesMinor,
      long otherDeductionsMinor,
      long tdsMinor,
      boolean pfApplicable,
      boolean esiApplicable,
      boolean ptApplicable) {}

  public record Result(
      long earnedGrossMinor,
      long lopDeductionMinor,
      long incentivesMinor,
      long pfMinor,
      long esiMinor,
      long ptMinor,
      long tdsMinor,
      long otherDeductionsMinor,
      long totalEarningsMinor,
      long totalDeductionsMinor,
      long netPayMinor) {

    /** Withholdings that become a liability other than net pay + TDS (PF + ESI + PT + other). */
    public long statutoryWithheldMinor() {
      return pfMinor + esiMinor + ptMinor + otherDeductionsMinor;
    }
  }

  public static Result compute(Input in, PayrollRates rates) {
    if (in.workingDays() <= 0) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "workingDays must be positive");
    }
    if (in.lopDays() < 0 || in.lopDays() > in.workingDays()) {
      throw new ApiException(
          ErrorCode.VALIDATION_FAILED, "lopDays must be within [0, workingDays]");
    }
    if (in.basicMinor() > in.grossMinor()) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "basic cannot exceed gross");
    }

    long lopDeduction = prorate(in.grossMinor(), in.lopDays(), in.workingDays());
    long earnedGross = in.grossMinor() - lopDeduction;
    long earnedBasic = in.basicMinor() - prorate(in.basicMinor(), in.lopDays(), in.workingDays());

    long pf = 0;
    if (in.pfApplicable()) {
      long pfWage = Math.min(earnedBasic, rates.pfWageCeilingMinor());
      pf = Money.ofMinor(pfWage).percent(rates.pfRatePercent()).toMinor();
    }
    long esi = 0;
    if (in.esiApplicable() && in.grossMinor() <= rates.esiGrossThresholdMinor()) {
      esi = Money.ofMinor(earnedGross).percent(rates.esiRatePercent()).toMinor();
    }
    long pt = in.ptApplicable() ? rates.ptFlatMinor() : 0;
    long tds = Math.max(0, in.tdsMinor());
    long other = Math.max(0, in.otherDeductionsMinor());

    long totalEarnings = earnedGross + Math.max(0, in.incentivesMinor());
    long totalDeductions = pf + esi + pt + tds + other;
    long netPay = totalEarnings - totalDeductions;
    if (netPay < 0) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "net pay would be negative");
    }

    return new Result(
        earnedGross,
        lopDeduction,
        Math.max(0, in.incentivesMinor()),
        pf,
        esi,
        pt,
        tds,
        other,
        totalEarnings,
        totalDeductions,
        netPay);
  }

  private static long prorate(long amountMinor, int lopDays, int workingDays) {
    if (lopDays == 0) {
      return 0;
    }
    // round(amount * lopDays / workingDays) with banker's rounding via Money.percent
    java.math.BigDecimal ratePercent =
        java.math.BigDecimal.valueOf(lopDays)
            .multiply(java.math.BigDecimal.valueOf(100))
            .divide(
                java.math.BigDecimal.valueOf(workingDays), 10, java.math.RoundingMode.HALF_EVEN);
    return Money.ofMinor(amountMinor).percent(ratePercent).toMinor();
  }
}
