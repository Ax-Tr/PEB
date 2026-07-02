package com.paywithease.compliance.infrastructure;

import com.paywithease.compliance.domain.SourceRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceRecordRepository extends JpaRepository<SourceRecord, String> {
  boolean existsByTenantIdAndRecordTypeAndSourceRef(
      String tenantId, String recordType, String sourceRef);

  List<SourceRecord> findByTenantIdAndYearAndMonth(String tenantId, int year, int month);
}
