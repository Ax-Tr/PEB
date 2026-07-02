package com.paywithease.compliance.infrastructure;

import com.paywithease.compliance.domain.ComplianceReportLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ComplianceReportLineRepository
    extends JpaRepository<ComplianceReportLine, String> {
  List<ComplianceReportLine> findByReportId(String reportId);

  @Transactional
  void deleteByReportId(String reportId);
}
