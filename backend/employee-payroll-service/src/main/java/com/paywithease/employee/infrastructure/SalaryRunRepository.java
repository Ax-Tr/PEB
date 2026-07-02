package com.paywithease.employee.infrastructure;

import com.paywithease.employee.domain.SalaryRun;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryRunRepository extends JpaRepository<SalaryRun, String> {
  boolean existsByTenantIdAndYearAndMonth(String tenantId, int year, int month);

  Optional<SalaryRun> findByTenantIdAndId(String tenantId, String id);
}
