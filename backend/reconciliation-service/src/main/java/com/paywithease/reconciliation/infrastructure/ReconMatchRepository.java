package com.paywithease.reconciliation.infrastructure;

import com.paywithease.reconciliation.domain.ReconMatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconMatchRepository extends JpaRepository<ReconMatch, String> {
  Optional<ReconMatch> findByTenantIdAndId(String tenantId, String id);

  List<ReconMatch> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status);

  // An external item already has a live proposal/confirmation (not rejected) → skip re-matching.
  boolean existsByTenantIdAndExternalItemIdAndStatusNot(
      String tenantId, String externalItemId, String status);
}
