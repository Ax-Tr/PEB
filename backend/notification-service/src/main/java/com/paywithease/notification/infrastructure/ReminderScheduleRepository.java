package com.paywithease.notification.infrastructure;

import com.paywithease.notification.domain.ReminderSchedule;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderScheduleRepository extends JpaRepository<ReminderSchedule, String> {

  // Due reminders across all tenants (the scheduled sender resolves tenant per-row).
  List<ReminderSchedule> findByStatusAndSendOnLessThanEqualOrderBySendOn(
      String status, LocalDate sendOn, Limit limit);

  boolean existsByTenantIdAndSourceRefAndEmiNumberAndOffsetDays(
      String tenantId, String sourceRef, Integer emiNumber, int offsetDays);

  List<ReminderSchedule> findByTenantIdAndSourceRefOrderBySendOn(String tenantId, String sourceRef);

  List<ReminderSchedule> findByTenantIdOrderBySendOnDesc(String tenantId);
}
