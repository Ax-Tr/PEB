package com.paywithease.notification.application;

import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Fires reminders whose send date has arrived. Runs daily (default 08:00 IST). The service resolves
 * the tenant per reminder row, so no {@code TenantContext} is set here.
 */
@Component
public class ReminderScheduler {

  private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

  private final NotificationService notificationService;

  public ReminderScheduler(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @Scheduled(cron = "${peb.notification.reminder-cron:0 0 8 * * *}")
  public void fireDueReminders() {
    int sent = notificationService.processDueReminders(LocalDate.now(IST), 500);
    log.info("Reminder sweep complete: {} reminder(s) sent", sent);
  }
}
