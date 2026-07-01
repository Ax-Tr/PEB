package com.paywithease.tenant.infrastructure;

import com.paywithease.tenant.domain.Business;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business, String> {
  boolean existsByGstinHash(String gstinHash);
}
