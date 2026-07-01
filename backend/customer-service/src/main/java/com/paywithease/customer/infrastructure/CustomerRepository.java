package com.paywithease.customer.infrastructure;

import com.paywithease.customer.domain.Customer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {

  Optional<Customer> findByTenantIdAndMobileHash(String tenantId, String mobileHash);

  boolean existsByTenantIdAndMobileHash(String tenantId, String mobileHash);

  List<Customer> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
