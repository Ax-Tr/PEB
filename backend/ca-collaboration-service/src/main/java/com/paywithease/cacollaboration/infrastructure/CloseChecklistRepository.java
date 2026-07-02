package com.paywithease.cacollaboration.infrastructure;

import com.paywithease.cacollaboration.domain.CloseChecklist;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CloseChecklistRepository extends JpaRepository<CloseChecklist, String> {

  Optional<CloseChecklist> findByTenantIdAndId(String tenantId, String id);

  Optional<CloseChecklist> findByTenantIdAndPeriodYearAndPeriodMonth(
      String tenantId, int periodYear, int periodMonth);
}
