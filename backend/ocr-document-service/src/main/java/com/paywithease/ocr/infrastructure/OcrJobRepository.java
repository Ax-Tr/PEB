package com.paywithease.ocr.infrastructure;

import com.paywithease.ocr.domain.OcrJob;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OcrJobRepository extends JpaRepository<OcrJob, String> {
  Optional<OcrJob> findByTenantIdAndId(String tenantId, String id);

  List<OcrJob> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
