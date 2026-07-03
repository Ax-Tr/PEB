package com.paywithease.ai.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoiceDraftRepository extends JpaRepository<VoiceDraft, String> {
  Optional<VoiceDraft> findByTenantIdAndId(String tenantId, String id);

  List<VoiceDraft> findByTenantIdOrderByCreatedAtDesc(String tenantId);

  List<VoiceDraft> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status);
}
