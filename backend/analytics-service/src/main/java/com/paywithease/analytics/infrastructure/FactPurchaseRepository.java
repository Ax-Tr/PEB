package com.paywithease.analytics.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FactPurchaseRepository extends JpaRepository<FactPurchase, String> {

  boolean existsByBillId(String billId);

  /** Direct cost (net of GST) for a period = sum of net value. */
  @Query(
      "select coalesce(sum(f.netMinor), 0) from FactPurchase f "
          + "where f.tenantId = :tenantId and f.periodYear = :year and f.periodMonth = :month")
  long sumCostMinor(
      @Param("tenantId") String tenantId, @Param("year") int year, @Param("month") int month);

  /** Outstanding (unpaid) purchase bills for payables aging. */
  @Query(
      "select f from FactPurchase f where f.tenantId = :tenantId and (f.totalMinor - f.amountPaidMinor) > 0")
  List<FactPurchase> findOutstanding(@Param("tenantId") String tenantId);
}
