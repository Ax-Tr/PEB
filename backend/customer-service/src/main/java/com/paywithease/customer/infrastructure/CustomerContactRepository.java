package com.paywithease.customer.infrastructure;

import com.paywithease.customer.domain.CustomerContact;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerContactRepository extends JpaRepository<CustomerContact, String> {

  List<CustomerContact> findByCustomerId(String customerId);
}
