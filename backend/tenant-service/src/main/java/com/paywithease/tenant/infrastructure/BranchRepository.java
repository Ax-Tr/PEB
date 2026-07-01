package com.paywithease.tenant.infrastructure;

import com.paywithease.tenant.domain.Branch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, String> {
  List<Branch> findByTenantId(String tenantId);
}
