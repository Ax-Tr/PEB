package com.paywithease.common.gst;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Exhaustive GST matrix (the hard gate) —
 * intra/inter/exempt/bill-of-supply/RCM/odd-split/multi-rate.
 */
class GstCalculatorTest {

  private static GstCalculator.LineInput line(long taxableMinor, String rate) {
    return new GstCalculator.LineInput(taxableMinor, new BigDecimal(rate));
  }

  @Test
  void intraStateSplitsIntoCgstAndSgst() {
    var r = GstCalculator.compute("27", "27", List.of(line(100000, "18")), true, false);
    assertThat(r.interState()).isFalse();
    assertThat(r.totalCgstMinor()).isEqualTo(9000);
    assertThat(r.totalSgstMinor()).isEqualTo(9000);
    assertThat(r.totalIgstMinor()).isZero();
    assertThat(r.totalTaxMinor()).isEqualTo(18000);
    assertThat(r.documentTotalMinor()).isEqualTo(118000);
  }

  @Test
  void interStateChargesIgstOnly() {
    var r = GstCalculator.compute("27", "29", List.of(line(100000, "18")), true, false);
    assertThat(r.interState()).isTrue();
    assertThat(r.totalIgstMinor()).isEqualTo(18000);
    assertThat(r.totalCgstMinor()).isZero();
    assertThat(r.documentTotalMinor()).isEqualTo(118000);
  }

  @Test
  void oddTaxSplitLosesNoPaise() {
    var r = GstCalculator.compute("27", "27", List.of(line(3, "18")), true, false);
    assertThat(r.totalTaxMinor()).isEqualTo(1);
    assertThat(r.totalCgstMinor()).isZero();
    assertThat(r.totalSgstMinor()).isEqualTo(1);
    assertThat(r.totalCgstMinor() + r.totalSgstMinor()).isEqualTo(r.totalTaxMinor());
  }

  @Test
  void exemptZeroRatedHasNoTax() {
    var r = GstCalculator.compute("27", "29", List.of(line(50000, "0")), true, false);
    assertThat(r.totalTaxMinor()).isZero();
    assertThat(r.documentTotalMinor()).isEqualTo(50000);
  }

  @Test
  void billOfSupplyNeverTaxesEvenWithARate() {
    var r = GstCalculator.compute("27", "27", List.of(line(100000, "18")), false, false);
    assertThat(r.totalTaxMinor()).isZero();
    assertThat(r.documentTotalMinor()).isEqualTo(100000);
  }

  @Test
  void reverseChargeComputesTaxButDoesNotCollectIt() {
    var r = GstCalculator.compute("27", "27", List.of(line(100000, "18")), true, true);
    assertThat(r.reverseCharge()).isTrue();
    assertThat(r.totalTaxMinor()).isEqualTo(18000);
    assertThat(r.documentTotalMinor()).isEqualTo(100000);
  }

  @Test
  void multiRateAggregatesSummaryByRate() {
    var r =
        GstCalculator.compute(
            "27",
            "27",
            List.of(line(100000, "18"), line(200000, "5"), line(50000, "18")),
            true,
            false);
    assertThat(r.summaryByRate()).hasSize(2);
    assertThat(r.totalTaxableMinor()).isEqualTo(350000);
    assertThat(r.totalTaxMinor()).isEqualTo(37000);
    assertThat(r.totalCgstMinor()).isEqualTo(18500);
    assertThat(r.totalSgstMinor()).isEqualTo(18500);
  }
}
