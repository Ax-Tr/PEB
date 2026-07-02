package com.paywithease.auditevidence.infrastructure;

import com.paywithease.auditevidence.domain.ExportJob;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExportJobRepository extends JpaRepository<ExportJob, String> {

  Optional<ExportJob> findByTenantIdAndId(String tenantId, String id);

  List<ExportJob> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
