package com.paywithease.reconciliation.infrastructure;

import com.paywithease.reconciliation.domain.ReconException;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconExceptionRepository extends JpaRepository<ReconException, String> {
  boolean existsByTenantIdAndItemId(String tenantId, String itemId);

  List<ReconException> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status);
}
