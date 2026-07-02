package com.paywithease.purchase.infrastructure;

import com.paywithease.purchase.domain.PurchaseBill;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseBillRepository extends JpaRepository<PurchaseBill, String> {

  Optional<PurchaseBill> findByTenantIdAndId(String tenantId, String id);

  List<PurchaseBill> findByTenantIdAndBillDateBetweenOrderByBillDateAsc(
      String tenantId, LocalDate from, LocalDate to);
}
