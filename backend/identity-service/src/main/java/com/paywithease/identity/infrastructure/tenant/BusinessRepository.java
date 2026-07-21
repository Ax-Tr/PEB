package com.paywithease.identity.infrastructure.tenant;

import com.paywithease.identity.domain.tenant.Business;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, String> {
  boolean existsByGstinHash(String gstinHash);
}
