package com.paywithease.identity.infrastructure;

import com.paywithease.identity.domain.DataConsentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRepository extends JpaRepository<DataConsentRecord, String> {}
