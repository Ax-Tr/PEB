package com.paywithease.cacollaboration.infrastructure;

import com.paywithease.cacollaboration.domain.ReportApproval;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportApprovalRepository extends JpaRepository<ReportApproval, String> {

  Optional<ReportApproval> findByTenantIdAndId(String tenantId, String id);

  List<ReportApproval> findByTenantIdAndReportTypeAndReportRef(
      String tenantId, String reportType, String reportRef);

  boolean existsByTenantIdAndReportTypeAndReportRefAndStatus(
      String tenantId, String reportType, String reportRef, String status);
}
