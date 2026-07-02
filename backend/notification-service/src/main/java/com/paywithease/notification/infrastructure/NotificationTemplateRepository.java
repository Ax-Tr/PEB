package com.paywithease.notification.infrastructure;

import com.paywithease.notification.domain.NotificationTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository
    extends JpaRepository<NotificationTemplate, String> {
  Optional<NotificationTemplate> findByTenantIdAndCodeAndChannel(
      String tenantId, String code, String channel);

  List<NotificationTemplate> findByTenantId(String tenantId);
}
