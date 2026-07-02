package com.paywithease.installment.infrastructure;

import com.paywithease.installment.domain.Installment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallmentRepository extends JpaRepository<Installment, String> {
  Optional<Installment> findByTenantIdAndId(String tenantId, String id);

  List<Installment> findByTenantIdAndTypeOrderByCreatedAtDesc(String tenantId, String type);

  boolean existsByTenantIdAndSourceRef(String tenantId, String sourceRef);
}
