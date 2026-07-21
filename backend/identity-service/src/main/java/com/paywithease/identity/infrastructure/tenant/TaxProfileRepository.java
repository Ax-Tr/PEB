package com.paywithease.identity.infrastructure.tenant;

import com.paywithease.identity.domain.tenant.BusinessTaxProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxProfileRepository extends JpaRepository<BusinessTaxProfile, String> {}
