package com.paywithease.ledger.infrastructure;

import com.paywithease.ledger.domain.LedgerBalance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerBalanceRepository extends JpaRepository<LedgerBalance, String> {
  Optional<LedgerBalance> findByTenantIdAndAccountId(String tenantId, String accountId);

  List<LedgerBalance> findByTenantId(String tenantId);
}
