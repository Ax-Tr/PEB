package com.paywithease.ocr.infrastructure;

import com.paywithease.ocr.domain.DocumentRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRecordRepository extends JpaRepository<DocumentRecord, String> {
  Optional<DocumentRecord> findByTenantIdAndId(String tenantId, String id);
}
