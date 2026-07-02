package com.paywithease.employee.domain.payroll;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paywithease.common.error.ApiException;
import org.junit.jupiter.api.Test;

/**
 * Exhaustive payroll matrix: PF ceiling, ESI threshold, LOP proration, incentives/TDS/other,
 * guards.
 */
class PayrollCalculatorTest {

  private final PayrollRates rates = PayrollRates.defaults();

  private PayrollCalculator.Input in(
      long gross,
      long basic,
      int workingDays,
      int lop,
      long incentives,
      long other,
      long tds,
      boolean pf,
      boolean esi,
      boolean pt) {
    return new PayrollCalculator.Input(
        gross, basic, workingDays, lop, incentives, other, tds, pf, esi, pt);
  }

  @Test
  void fullMonthWithPfEsiPt() {
    // gross ₹20,000, basic ₹10,000; PF 12% of 10,000 = 1,200; ESI 0.75% of 20,000 = 150; PT ₹200
    var r =
        PayrollCalculator.compute(
            in(2_000_000, 1_000_000, 30, 0, 0, 0, 0, true, true, true), rates);
    assertThat(r.pfMinor()).isEqualTo(120_000);
    assertThat(r.esiMinor()).isEqualTo(15_000);
    assertThat(r.ptMinor()).isEqualTo(20_000);
    assertThat(r.totalDeductionsMinor()).isEqualTo(155_000);
    assertThat(r.netPayMinor()).isEqualTo(1_845_000);
  }

  @Test
  void esiNotApplicableAboveGrossThreshold() {
    // gross ₹25,000 > ₹21,000 threshold -> ESI 0
    var r =
        PayrollCalculator.compute(
            in(2_500_000, 1_200_000, 30, 0, 0, 0, 0, true, true, true), rates);
    assertThat(r.esiMinor()).isZero();
    assertThat(r.pfMinor()).isEqualTo(144_000); // 12% of ₹12,000
    assertThat(r.netPayMinor()).isEqualTo(2_500_000 - 144_000 - 20_000);
  }

  @Test
  void pfCappedAtWageCeiling() {
    // basic ₹20,000 > ₹15,000 ceiling -> PF on ₹15,000 = 1,800
    var r =
        PayrollCalculator.compute(
            in(3_000_000, 2_000_000, 30, 0, 0, 0, 0, true, true, true), rates);
    assertThat(r.pfMinor()).isEqualTo(180_000);
  }

  @Test
  void lopProratesGrossAndBasic() {
    // gross ₹30,000, 3 LOP of 30 -> LOP ₹3,000; earned gross ₹27,000
    var r =
        PayrollCalculator.compute(
            in(3_000_000, 1_500_000, 30, 3, 0, 0, 0, true, true, true), rates);
    assertThat(r.lopDeductionMinor()).isEqualTo(300_000);
    assertThat(r.earnedGrossMinor()).isEqualTo(2_700_000);
    assertThat(r.pfMinor()).isEqualTo(162_000); // 12% of earned basic ₹13,500
  }

  @Test
  void incentivesTdsAndOtherDeductions() {
    var r =
        PayrollCalculator.compute(
            in(2_000_000, 1_000_000, 30, 0, 500_000, 100_000, 200_000, true, true, true), rates);
    assertThat(r.totalEarningsMinor()).isEqualTo(2_500_000); // gross + incentive
    assertThat(r.tdsMinor()).isEqualTo(200_000);
    assertThat(r.netPayMinor())
        .isEqualTo(2_500_000 - 120_000 - 15_000 - 20_000 - 200_000 - 100_000);
  }

  @Test
  void ledgerIdentityHolds() {
    // The ledger relies on: totalEarnings == net + statutoryWithheld + tds
    var r =
        PayrollCalculator.compute(
            in(2_000_000, 1_000_000, 30, 0, 500_000, 100_000, 200_000, true, true, true), rates);
    assertThat(r.netPayMinor() + r.statutoryWithheldMinor() + r.tdsMinor())
        .isEqualTo(r.totalEarningsMinor());
  }

  @Test
  void negativeNetIsRejected() {
    assertThatThrownBy(
            () ->
                PayrollCalculator.compute(
                    in(100_000, 100_000, 30, 0, 0, 500_000, 0, false, false, false), rates))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("net pay");
  }

  @Test
  void invalidLopIsRejected() {
    assertThatThrownBy(
            () ->
                PayrollCalculator.compute(
                    in(2_000_000, 1_000_000, 30, 31, 0, 0, 0, true, true, true), rates))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("lopDays");
  }
}
