package com.paywithease.auditevidence.infrastructure;

import com.paywithease.auditevidence.domain.EvidenceItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceItemRepository extends JpaRepository<EvidenceItem, String> {

  Optional<EvidenceItem> findByTenantIdAndId(String tenantId, String id);

  List<EvidenceItem> findByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
      String tenantId, String entityType, String entityId);

  boolean existsByTenantIdAndEntityTypeAndEntityIdAndContentHash(
      String tenantId, String entityType, String entityId, String contentHash);
}
