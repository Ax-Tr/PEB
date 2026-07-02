package com.paywithease.ai.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnomalyAlertRepository extends JpaRepository<AnomalyAlert, String> {

  Optional<AnomalyAlert> findByTenantIdAndId(String tenantId, String id);

  List<AnomalyAlert> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status);

  List<AnomalyAlert> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
