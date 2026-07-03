package com.paywithease.notification.infrastructure;

import com.paywithease.notification.domain.DeviceToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, String> {

  Optional<DeviceToken> findByTenantIdAndToken(String tenantId, String token);

  List<DeviceToken> findByTenantIdAndActiveTrueOrderByUpdatedAtDesc(String tenantId);
}
