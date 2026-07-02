package com.paywithease.reconciliation.infrastructure;

import com.paywithease.reconciliation.domain.ReconItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconItemRepository extends JpaRepository<ReconItem, String> {
  Optional<ReconItem> findByTenantIdAndId(String tenantId, String id);

  Optional<ReconItem> findByTenantIdAndSideAndSourceTypeAndSourceRef(
      String tenantId, String side, String sourceType, String sourceRef);

  List<ReconItem> findByTenantIdAndSideAndMatchedFalse(String tenantId, String side);

  List<ReconItem> findByTenantIdAndSideAndDirectionAndMatchedFalse(
      String tenantId, String side, String direction);
}
