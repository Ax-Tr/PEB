package com.paywithease.product.infrastructure;

import com.paywithease.product.domain.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {
  List<Product> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
