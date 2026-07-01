package com.paywithease.tenant.infrastructure;

import com.paywithease.tenant.domain.BusinessSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsRepository extends JpaRepository<BusinessSettings, String> {}
