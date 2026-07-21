package com.paywithease.business.customer.infrastructure;

import com.paywithease.business.customer.domain.Customer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
  boolean existsByTenantIdAndMobileHash(String tenantId, String mobileHash);

  Optional<Customer> findByTenantIdAndMobileHash(String tenantId, String mobileHash);

  List<Customer> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
