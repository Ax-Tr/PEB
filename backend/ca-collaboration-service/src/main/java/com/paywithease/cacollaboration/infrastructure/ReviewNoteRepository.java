package com.paywithease.cacollaboration.infrastructure;

import com.paywithease.cacollaboration.domain.ReviewNote;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewNoteRepository extends JpaRepository<ReviewNote, String> {

  List<ReviewNote> findByTenantIdAndEntityTypeAndEntityIdOrderByCreatedAtAsc(
      String tenantId, String entityType, String entityId);
}
