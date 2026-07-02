package com.paywithease.analytics.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure cashflow rollup. Given an opening balance and an ordered list of per-period inflow/outflow
 * totals (paise), produces each period's net movement and running closing balance. Inflows come
 * from confirmed collections; outflows from completed payouts.
 */
public final class CashflowCalculator {

  private CashflowCalculator() {}

  public record PeriodFlow(int year, int month, long inflowMinor, long outflowMinor) {}

  public record PeriodResult(
      int year,
      int month,
      long inflowMinor,
      long outflowMinor,
      long netMinor,
      long closingBalanceMinor) {}

  public record Cashflow(
      long openingBalanceMinor,
      long totalInflowMinor,
      long totalOutflowMinor,
      long netMinor,
      long closingBalanceMinor,
      List<PeriodResult> periods) {}

  /** Periods must be supplied in chronological order; the running balance is carried forward. */
  public static Cashflow compute(long openingBalanceMinor, List<PeriodFlow> periods) {
    long running = openingBalanceMinor;
    long totalIn = 0;
    long totalOut = 0;
    List<PeriodResult> results = new ArrayList<>(periods.size());
    for (PeriodFlow p : periods) {
      long net = p.inflowMinor() - p.outflowMinor();
      running += net;
      totalIn += p.inflowMinor();
      totalOut += p.outflowMinor();
      results.add(
          new PeriodResult(p.year(), p.month(), p.inflowMinor(), p.outflowMinor(), net, running));
    }
    return new Cashflow(
        openingBalanceMinor, totalIn, totalOut, totalIn - totalOut, running, List.copyOf(results));
  }
}
