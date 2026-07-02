package com.paywithease.analytics.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProfitCalculatorTest {

  @Test
  void computesGrossAndNetProfitAndMargins() {
    var pnl = ProfitCalculator.compute(1_000_000, 600_000, 150_000);
    assertThat(pnl.grossProfitMinor()).isEqualTo(400_000);
    assertThat(pnl.netProfitMinor()).isEqualTo(250_000);
    assertThat(pnl.grossMarginPct()).isEqualByComparingTo(new BigDecimal("40.00"));
    assertThat(pnl.netMarginPct()).isEqualByComparingTo(new BigDecimal("25.00"));
  }

  @Test
  void zeroRevenueYieldsZeroMarginNotDivideByZero() {
    var pnl = ProfitCalculator.compute(0, 500_000, 0);
    assertThat(pnl.grossProfitMinor()).isEqualTo(-500_000);
    assertThat(pnl.grossMarginPct()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(pnl.netMarginPct()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void lossShowsNegativeMargin() {
    var pnl = ProfitCalculator.compute(1_000_000, 900_000, 200_000);
    assertThat(pnl.netProfitMinor()).isEqualTo(-100_000);
    assertThat(pnl.netMarginPct()).isEqualByComparingTo(new BigDecimal("-10.00"));
  }
}
