package com.paywithease.tenant.infrastructure;

import com.paywithease.tenant.domain.BusinessTaxProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxProfileRepository extends JpaRepository<BusinessTaxProfile, String> {}
