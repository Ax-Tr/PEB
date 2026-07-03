package com.paywithease.payout.infrastructure;

import com.paywithease.payout.domain.Payout;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutRepository extends JpaRepository<Payout, String> {
  Optional<Payout> findByTenantIdAndId(String tenantId, String id);

  List<Payout> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
