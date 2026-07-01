package com.paywithease.common.idempotency;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, IdempotencyKey.PK> {
  Optional<IdempotencyKey> findByTenantIdAndKey(String tenantId, String key);
}
