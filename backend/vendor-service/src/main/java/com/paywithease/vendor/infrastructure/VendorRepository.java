package com.paywithease.vendor.infrastructure;

import com.paywithease.vendor.domain.Vendor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, String> {
  List<Vendor> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
