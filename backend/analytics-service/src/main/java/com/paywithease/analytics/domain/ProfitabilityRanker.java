package com.paywithease.analytics.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

/**
 * Pure product/service profitability ranking. Sorts items by profit (revenue − cost) descending and
 * computes each item's margin. Used for "most / least profitable product" dashboards.
 *
 * <p>Populated only when the source invoice events carry line-level detail (product id, revenue,
 * cost). Until upstream events are enriched, the read-model that feeds this may be empty — the
 * ranking itself is correct regardless.
 */
public final class ProfitabilityRanker {

  private ProfitabilityRanker() {}

  public record Item(String productId, String productName, long revenueMinor, long costMinor) {}

  public record Ranked(
      String productId,
      String productName,
      long revenueMinor,
      long costMinor,
      long profitMinor,
      BigDecimal marginPct) {}

  public static List<Ranked> rank(List<Item> items) {
    return items.stream()
        .map(
            i -> {
              long profit = i.revenueMinor() - i.costMinor();
              return new Ranked(
                  i.productId(),
                  i.productName(),
                  i.revenueMinor(),
                  i.costMinor(),
                  profit,
                  marginPct(profit, i.revenueMinor()));
            })
        .sorted(Comparator.comparingLong(Ranked::profitMinor).reversed())
        .toList();
  }

  private static BigDecimal marginPct(long profitMinor, long revenueMinor) {
    if (revenueMinor == 0) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }
    return BigDecimal.valueOf(profitMinor)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(revenueMinor), 2, RoundingMode.HALF_EVEN);
  }
}
