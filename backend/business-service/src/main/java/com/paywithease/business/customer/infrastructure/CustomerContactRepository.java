package com.paywithease.business.customer.infrastructure;

import com.paywithease.business.customer.domain.CustomerContact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerContactRepository extends JpaRepository<CustomerContact, String> {
  List<CustomerContact> findByCustomerId(String customerId);
}
