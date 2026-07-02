package com.paywithease.purchase.infrastructure;

import com.paywithease.purchase.domain.Expense;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseRepository extends JpaRepository<Expense, String> {

  Optional<Expense> findByTenantIdAndId(String tenantId, String id);

  List<Expense> findByTenantIdOrderByExpenseDateDesc(String tenantId);
}
