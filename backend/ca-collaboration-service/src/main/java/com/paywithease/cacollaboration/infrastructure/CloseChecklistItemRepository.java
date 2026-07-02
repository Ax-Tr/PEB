package com.paywithease.cacollaboration.infrastructure;

import com.paywithease.cacollaboration.domain.CloseChecklistItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CloseChecklistItemRepository extends JpaRepository<CloseChecklistItem, String> {

  List<CloseChecklistItem> findByChecklistIdOrderBySortOrderAsc(String checklistId);

  Optional<CloseChecklistItem> findByTenantIdAndId(String tenantId, String id);
}
