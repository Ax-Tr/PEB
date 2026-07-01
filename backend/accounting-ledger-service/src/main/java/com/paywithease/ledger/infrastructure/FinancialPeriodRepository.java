package com.paywithease.ledger.infrastructure;

import com.paywithease.ledger.domain.FinancialPeriod;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialPeriodRepository extends JpaRepository<FinancialPeriod, String> {
  Optional<FinancialPeriod> findByTenantIdAndYearAndMonth(String tenantId, int year, int month);
}
