package com.paywithease.analytics.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FactExpenseRepository extends JpaRepository<FactExpense, String> {

  boolean existsByExpenseId(String expenseId);

  @Query(
      "select coalesce(sum(f.amountMinor), 0) from FactExpense f "
          + "where f.tenantId = :tenantId and f.periodYear = :year and f.periodMonth = :month")
  long sumExpenseMinor(
      @Param("tenantId") String tenantId, @Param("year") int year, @Param("month") int month);
}
