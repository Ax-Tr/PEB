package com.paywithease.ai.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiFeedbackRepository extends JpaRepository<AiFeedback, String> {

  List<AiFeedback> findByTenantIdAndSuggestionId(String tenantId, String suggestionId);
}
