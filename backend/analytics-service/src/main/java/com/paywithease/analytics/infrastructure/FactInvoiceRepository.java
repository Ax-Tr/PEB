package com.paywithease.analytics.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FactInvoiceRepository extends JpaRepository<FactInvoice, String> {

  boolean existsByInvoiceId(String invoiceId);

  /** Revenue recognised (net of GST) for a period = sum of taxable value. */
  @Query(
      "select coalesce(sum(f.taxableMinor), 0) from FactInvoice f "
          + "where f.tenantId = :tenantId and f.periodYear = :year and f.periodMonth = :month")
  long sumRevenueMinor(
      @Param("tenantId") String tenantId, @Param("year") int year, @Param("month") int month);

  /** Outstanding (unpaid) invoices for receivables aging. */
  @Query(
      "select f from FactInvoice f where f.tenantId = :tenantId and (f.totalMinor - f.amountPaidMinor) > 0")
  List<FactInvoice> findOutstanding(@Param("tenantId") String tenantId);
}
