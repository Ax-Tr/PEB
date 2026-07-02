package com.paywithease.notification.infrastructure;

import com.paywithease.notification.domain.NotificationLog;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, String> {
  Optional<NotificationLog> findByProviderAndProviderRef(String provider, String providerRef);

  List<NotificationLog> findTop100ByTenantIdOrderByCreatedAtDesc(String tenantId);
}
