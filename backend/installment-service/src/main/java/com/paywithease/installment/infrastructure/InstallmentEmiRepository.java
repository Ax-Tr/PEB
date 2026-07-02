package com.paywithease.installment.infrastructure;

import com.paywithease.installment.domain.InstallmentEmi;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstallmentEmiRepository extends JpaRepository<InstallmentEmi, String> {
  List<InstallmentEmi> findByInstallmentIdOrderByEmiNumber(String installmentId);

  // Upcoming/overdue EMIs for reminder scheduling (consumed by the notification service in Sprint
  // 9).
  List<InstallmentEmi> findByTenantIdAndStatusNotAndDueDateLessThanEqualOrderByDueDate(
      String tenantId, String status, LocalDate dueOnOrBefore);
}
