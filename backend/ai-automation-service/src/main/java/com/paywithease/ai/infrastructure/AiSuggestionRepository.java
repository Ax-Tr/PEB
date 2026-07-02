package com.paywithease.ai.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSuggestionRepository extends JpaRepository<AiSuggestion, String> {

  Optional<AiSuggestion> findByTenantIdAndId(String tenantId, String id);

  List<AiSuggestion> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status);

  List<AiSuggestion> findByTenantIdOrderByCreatedAtDesc(String tenantId);

  boolean existsByTenantIdAndSubjectTypeAndSubjectIdAndKind(
      String tenantId, String subjectType, String subjectId, String kind);
}
