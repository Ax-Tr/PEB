package com.paywithease.payout.infrastructure;

import com.paywithease.payout.domain.Beneficiary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, String> {
  Optional<Beneficiary> findByTenantIdAndId(String tenantId, String id);

  List<Beneficiary> findByTenantIdAndPartyTypeAndPartyId(
      String tenantId, String partyType, String partyId);
}
