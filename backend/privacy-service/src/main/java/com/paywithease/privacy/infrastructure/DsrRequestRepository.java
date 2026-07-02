package com.paywithease.privacy.infrastructure;

import com.paywithease.privacy.domain.DsrRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DsrRequestRepository extends JpaRepository<DsrRequest, String> {

  Optional<DsrRequest> findByTenantIdAndId(String tenantId, String id);

  List<DsrRequest> findByTenantIdOrderByReceivedAtDesc(String tenantId);

  List<DsrRequest> findByTenantIdAndStatusOrderByReceivedAtDesc(String tenantId, String status);
}
