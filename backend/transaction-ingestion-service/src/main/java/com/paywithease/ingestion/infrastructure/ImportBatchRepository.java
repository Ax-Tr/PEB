package com.paywithease.ingestion.infrastructure;

import com.paywithease.ingestion.domain.ImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, String> {}
