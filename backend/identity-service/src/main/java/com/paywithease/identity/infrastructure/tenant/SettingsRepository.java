package com.paywithease.identity.infrastructure.tenant;

import com.paywithease.identity.domain.tenant.BusinessSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsRepository extends JpaRepository<BusinessSettings, String> {}
