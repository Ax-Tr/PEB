package com.paywithease.cacollaboration.infrastructure;

import com.paywithease.cacollaboration.domain.CaInvite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaInviteRepository extends JpaRepository<CaInvite, String> {

  Optional<CaInvite> findByTenantIdAndId(String tenantId, String id);

  List<CaInvite> findByTenantIdOrderByCreatedAtDesc(String tenantId);

  List<CaInvite> findByTenantIdAndLinkedUserId(String tenantId, String linkedUserId);
}
