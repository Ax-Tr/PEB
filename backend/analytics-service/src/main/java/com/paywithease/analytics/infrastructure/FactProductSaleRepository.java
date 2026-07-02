package com.paywithease.analytics.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FactProductSaleRepository extends JpaRepository<FactProductSale, String> {

  @Query(
      "select f from FactProductSale f "
          + "where f.tenantId = :tenantId and f.periodYear = :year and f.periodMonth = :month")
  List<FactProductSale> findForPeriod(
      @Param("tenantId") String tenantId, @Param("year") int year, @Param("month") int month);
}
