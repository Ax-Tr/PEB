package com.paywithease.analytics.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgingCalculatorTest {

  private static final LocalDate AS_OF = LocalDate.of(2026, 7, 1);

  private AgingCalculator.AgingItem item(long minor, LocalDate date) {
    return new AgingCalculator.AgingItem(minor, date);
  }

  private long bucket(AgingCalculator.Aging a, AgingCalculator.Bucket b) {
    return a.buckets().stream().filter(x -> x.bucket() == b).findFirst().orElseThrow().totalMinor();
  }

  @Test
  void bucketsByDaysOutstanding() {
    var aging =
        AgingCalculator.compute(
            List.of(
                item(100_000, AS_OF.minusDays(10)), // 0-30
                item(200_000, AS_OF.minusDays(45)), // 31-60
                item(300_000, AS_OF.minusDays(75)), // 61-90
                item(400_000, AS_OF.minusDays(200))), // 90+
            AS_OF);
    assertThat(bucket(aging, AgingCalculator.Bucket.DAYS_0_30)).isEqualTo(100_000);
    assertThat(bucket(aging, AgingCalculator.Bucket.DAYS_31_60)).isEqualTo(200_000);
    assertThat(bucket(aging, AgingCalculator.Bucket.DAYS_61_90)).isEqualTo(300_000);
    assertThat(bucket(aging, AgingCalculator.Bucket.DAYS_90_PLUS)).isEqualTo(400_000);
    assertThat(aging.totalOutstandingMinor()).isEqualTo(1_000_000);
    assertThat(aging.totalCount()).isEqualTo(4);
  }

  @Test
  void boundaryAt30And90DaysStaysInLowerBucket() {
    var aging =
        AgingCalculator.compute(
            List.of(item(500, AS_OF.minusDays(30)), item(700, AS_OF.minusDays(90))), AS_OF);
    assertThat(bucket(aging, AgingCalculator.Bucket.DAYS_0_30)).isEqualTo(500);
    assertThat(bucket(aging, AgingCalculator.Bucket.DAYS_61_90)).isEqualTo(700);
    assertThat(bucket(aging, AgingCalculator.Bucket.DAYS_90_PLUS)).isZero();
  }

  @Test
  void ignoresNonPositiveAndNullDatedItems() {
    var aging =
        AgingCalculator.compute(
            List.of(item(0, AS_OF), item(-100, AS_OF.minusDays(5)), item(100, null)), AS_OF);
    assertThat(aging.totalOutstandingMinor()).isZero();
    assertThat(aging.totalCount()).isZero();
  }

  @Test
  void futureDatedItemsFallInCurrentBucket() {
    var aging = AgingCalculator.compute(List.of(item(900, AS_OF.plusDays(5))), AS_OF);
    assertThat(bucket(aging, AgingCalculator.Bucket.DAYS_0_30)).isEqualTo(900);
  }
}
