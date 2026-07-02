package com.paywithease.analytics.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Pure receivables/payables aging. Buckets outstanding items by the number of days between a
 * reference date (invoice/bill date) and the "as of" date. Standard MSME buckets: 0–30, 31–60,
 * 61–90, and 90+ days.
 *
 * <p>Note: true aging would bucket by <i>due date</i>. Upstream events currently carry only the
 * document date, so this ages by days-outstanding from the document date — a legitimate and common
 * MSME view, flagged so a due-date feed can refine it later.
 */
public final class AgingCalculator {

  private AgingCalculator() {}

  public enum Bucket {
    DAYS_0_30,
    DAYS_31_60,
    DAYS_61_90,
    DAYS_90_PLUS
  }

  /**
   * An outstanding amount and the date it started aging from. Negative/zero amounts are ignored.
   */
  public record AgingItem(long outstandingMinor, LocalDate referenceDate) {}

  public record BucketTotal(Bucket bucket, long totalMinor, int count) {}

  public record Aging(List<BucketTotal> buckets, long totalOutstandingMinor, int totalCount) {}

  public static Aging compute(List<AgingItem> items, LocalDate asOf) {
    long[] totals = new long[Bucket.values().length];
    int[] counts = new int[Bucket.values().length];
    long grand = 0;
    int grandCount = 0;
    for (AgingItem item : items) {
      if (item.outstandingMinor() <= 0 || item.referenceDate() == null) {
        continue;
      }
      long days = ChronoUnit.DAYS.between(item.referenceDate(), asOf);
      int idx = bucketFor(days).ordinal();
      totals[idx] += item.outstandingMinor();
      counts[idx]++;
      grand += item.outstandingMinor();
      grandCount++;
    }
    List<BucketTotal> out =
        List.of(
            new BucketTotal(Bucket.DAYS_0_30, totals[0], counts[0]),
            new BucketTotal(Bucket.DAYS_31_60, totals[1], counts[1]),
            new BucketTotal(Bucket.DAYS_61_90, totals[2], counts[2]),
            new BucketTotal(Bucket.DAYS_90_PLUS, totals[3], counts[3]));
    return new Aging(out, grand, grandCount);
  }

  private static Bucket bucketFor(long days) {
    if (days <= 30) {
      return Bucket.DAYS_0_30; // includes not-yet-aged / future-dated items
    }
    if (days <= 60) {
      return Bucket.DAYS_31_60;
    }
    if (days <= 90) {
      return Bucket.DAYS_61_90;
    }
    return Bucket.DAYS_90_PLUS;
  }
}
