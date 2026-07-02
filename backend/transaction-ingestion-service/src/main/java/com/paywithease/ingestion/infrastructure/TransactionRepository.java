package com.paywithease.ingestion.infrastructure;

import com.paywithease.ingestion.domain.Transaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
  Optional<Transaction> findByTenantIdAndId(String tenantId, String id);

  boolean existsByTenantIdAndDedupeHash(String tenantId, String dedupeHash);

  List<Transaction> findByTenantIdAndClassificationStatusOrderByTxnDateDesc(
      String tenantId, String classificationStatus);

  List<Transaction> findTop200ByTenantIdOrderByTxnDateDesc(String tenantId);
}
