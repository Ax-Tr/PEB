package com.paywithease.compliance.infrastructure;

import com.paywithease.compliance.domain.ComplianceReport;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceReportRepository extends JpaRepository<ComplianceReport, String> {
  Optional<ComplianceReport> findByTenantIdAndId(String tenantId, String id);

  Optional<ComplianceReport> findByTenantIdAndTypeAndYearAndMonth(
      String tenantId, String type, int year, int month);

  List<ComplianceReport> findByTenantIdOrderByGeneratedAtDesc(String tenantId);
}
