package com.paywithease.identity.infrastructure;

import com.paywithease.identity.domain.OtpAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OtpAuditRepository extends JpaRepository<OtpAudit, String> {}
