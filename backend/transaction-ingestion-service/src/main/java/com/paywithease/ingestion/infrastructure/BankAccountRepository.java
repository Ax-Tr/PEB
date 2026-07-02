package com.paywithease.ingestion.infrastructure;

import com.paywithease.ingestion.domain.BankAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankAccountRepository extends JpaRepository<BankAccount, String> {
  Optional<BankAccount> findByTenantIdAndId(String tenantId, String id);

  List<BankAccount> findByTenantIdOrderByCreatedAtDesc(String tenantId);

  boolean existsByTenantIdAndAccountNumberHash(String tenantId, String accountNumberHash);
}
