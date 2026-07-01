package com.paywithease.ledger.infrastructure;

import com.paywithease.ledger.domain.ChartOfAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChartOfAccountRepository extends JpaRepository<ChartOfAccount, String> {
  Optional<ChartOfAccount> findByTenantIdAndCode(String tenantId, String code);

  List<ChartOfAccount> findByTenantIdOrderByCode(String tenantId);

  boolean existsByTenantId(String tenantId);
}
