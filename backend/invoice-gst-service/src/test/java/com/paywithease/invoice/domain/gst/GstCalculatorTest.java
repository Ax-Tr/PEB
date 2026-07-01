package com.paywithease.invoice.domain.gst;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

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
    assertThat(r.totalIgstMinor()).isEqualTo(0);
    assertThat(r.totalTaxMinor()).isEqualTo(18000);
    assertThat(r.invoiceTotalMinor()).isEqualTo(118000);
  }

  @Test
  void interStateChargesIgstOnly() {
    var r = GstCalculator.compute("27", "29", List.of(line(100000, "18")), true, false);
    assertThat(r.interState()).isTrue();
    assertThat(r.totalIgstMinor()).isEqualTo(18000);
    assertThat(r.totalCgstMinor()).isZero();
    assertThat(r.totalSgstMinor()).isZero();
    assertThat(r.invoiceTotalMinor()).isEqualTo(118000);
  }

  @Test
  void oddTaxSplitLosesNoPaise() {
    // 3 paise @ 18% = 0.54 paise -> rounds to 1 paise; split must stay exact: 0 + 1.
    var r = GstCalculator.compute("27", "27", List.of(line(3, "18")), true, false);
    assertThat(r.totalTaxMinor()).isEqualTo(1);
    assertThat(r.totalCgstMinor()).isEqualTo(0);
    assertThat(r.totalSgstMinor()).isEqualTo(1);
    assertThat(r.totalCgstMinor() + r.totalSgstMinor()).isEqualTo(r.totalTaxMinor());
  }

  @Test
  void exemptZeroRatedHasNoTax() {
    var r = GstCalculator.compute("27", "29", List.of(line(50000, "0")), true, false);
    assertThat(r.totalTaxMinor()).isZero();
    assertThat(r.invoiceTotalMinor()).isEqualTo(50000);
  }

  @Test
  void billOfSupplyNeverTaxesEvenWithARate() {
    var r = GstCalculator.compute("27", "27", List.of(line(100000, "18")), false, false);
    assertThat(r.totalTaxMinor()).isZero();
    assertThat(r.invoiceTotalMinor()).isEqualTo(100000);
  }

  @Test
  void reverseChargeComputesTaxButDoesNotCollectIt() {
    var r = GstCalculator.compute("27", "27", List.of(line(100000, "18")), true, true);
    assertThat(r.reverseCharge()).isTrue();
    assertThat(r.totalTaxMinor()).isEqualTo(18000); // reported for the recipient
    assertThat(r.invoiceTotalMinor()).isEqualTo(100000); // not collected here
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
    // Two rate groups: 18% (150000 taxable) and 5% (200000 taxable).
    assertThat(r.summaryByRate()).hasSize(2);
    long taxable18 =
        r.summaryByRate().stream()
            .filter(s -> s.gstRatePercent().compareTo(new BigDecimal("18")) == 0)
            .mapToLong(GstCalculator.RateSummary::taxableValueMinor)
            .sum();
    assertThat(taxable18).isEqualTo(150000);
    assertThat(r.totalTaxableMinor()).isEqualTo(350000);
    // 18%: tax on 150000 = 27000 (13500+13500); 5%: tax on 200000 = 10000 (5000+5000)
    assertThat(r.totalTaxMinor()).isEqualTo(37000);
    assertThat(r.totalCgstMinor()).isEqualTo(18500);
    assertThat(r.totalSgstMinor()).isEqualTo(18500);
  }
}
