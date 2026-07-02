package com.paywithease.analytics.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FactCashMovementRepository extends JpaRepository<FactCashMovement, String> {

  boolean existsByMovementId(String movementId);

  /** Per-period, per-direction cash totals for the cashflow rollup (grouped in the store). */
  @Query(
      "select f.periodYear as periodYear, f.periodMonth as periodMonth, "
          + "f.direction as direction, coalesce(sum(f.amountMinor), 0) as amountMinor "
          + "from FactCashMovement f where f.tenantId = :tenantId "
          + "group by f.periodYear, f.periodMonth, f.direction "
          + "order by f.periodYear, f.periodMonth")
  List<PeriodDirectionSum> aggregateByPeriodAndDirection(@Param("tenantId") String tenantId);

  /** Projection for the grouped cashflow query. */
  interface PeriodDirectionSum {
    int getPeriodYear();

    int getPeriodMonth();

    String getDirection();

    long getAmountMinor();
  }
}
