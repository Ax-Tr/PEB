package com.paywithease.analytics.infrastructure;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreamWatermarkRepository extends JpaRepository<StreamWatermark, String> {
  List<StreamWatermark> findByTenantId(String tenantId);
}
