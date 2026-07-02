package com.paywithease.analytics.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure profit &amp; margin computation from period totals (all amounts in paise). Produces an
 * indicative gross and net profit with margin percentages. This is a management view derived from
 * event-fed read-models — it is not the statutory P&amp;L, which the ledger owns.
 */
public final class ProfitCalculator {

  private ProfitCalculator() {}

  /**
   * @param revenueMinor recognised sales (net of GST) for the period
   * @param directCostMinor cost of purchases (net of GST) for the period
   * @param operatingExpenseMinor approved operating expenses for the period
   */
  public record Pnl(
      long revenueMinor,
      long directCostMinor,
      long operatingExpenseMinor,
      long grossProfitMinor,
      long netProfitMinor,
      BigDecimal grossMarginPct,
      BigDecimal netMarginPct) {}

  public static Pnl compute(long revenueMinor, long directCostMinor, long operatingExpenseMinor) {
    long grossProfit = revenueMinor - directCostMinor;
    long netProfit = grossProfit - operatingExpenseMinor;
    return new Pnl(
        revenueMinor,
        directCostMinor,
        operatingExpenseMinor,
        grossProfit,
        netProfit,
        marginPct(grossProfit, revenueMinor),
        marginPct(netProfit, revenueMinor));
  }

  /**
   * Margin as a percentage of revenue, 2 dp, banker's rounding. Zero revenue → 0 (not a divide).
   */
  private static BigDecimal marginPct(long profitMinor, long revenueMinor) {
    if (revenueMinor == 0) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }
    return BigDecimal.valueOf(profitMinor)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(revenueMinor), 2, RoundingMode.HALF_EVEN);
  }
}
