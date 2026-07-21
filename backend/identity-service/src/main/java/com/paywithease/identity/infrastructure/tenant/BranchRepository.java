package com.paywithease.identity.infrastructure.tenant;

import com.paywithease.identity.domain.tenant.Branch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, String> {
  List<Branch> findByTenantId(String tenantId);
}
