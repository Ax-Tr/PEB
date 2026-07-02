package com.paywithease.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.notification.domain.Channel;
import com.paywithease.notification.domain.NotificationLog;
import com.paywithease.notification.domain.NotificationTemplate;
import com.paywithease.notification.domain.ReminderSchedule;
import com.paywithease.notification.infrastructure.NotificationLogRepository;
import com.paywithease.notification.infrastructure.NotificationTemplateRepository;
import com.paywithease.notification.infrastructure.ReminderScheduleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

  @Mock NotificationTemplateRepository templates;
  @Mock NotificationLogRepository logs;
  @Mock ReminderScheduleRepository reminders;
  @Mock ChannelRouter router;
  @Mock NotificationChannel channel;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private NotificationService service;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service =
        new NotificationService(
            templates, logs, reminders, router, audit, outbox, objectMapper, clock, 2);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
    when(logs.save(any())).thenAnswer(returnsFirstArg());
    when(reminders.save(any())).thenAnswer(returnsFirstArg());
    when(router.forChannel(any())).thenReturn(channel);
    when(templates.findByTenantIdAndCodeAndChannel("tenant1", "EMI_DUE", "SMS"))
        .thenReturn(Optional.of(template()));
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  private NotificationTemplate template() {
    return new NotificationTemplate(
        "t1",
        "tenant1",
        "EMI_DUE",
        Channel.SMS,
        null,
        "Hi {{name}}, {{amount}} due",
        clock.instant());
  }

  @Test
  void sendTemplatedMarksSentWhenAccepted() {
    when(channel.send(any(), any(), any()))
        .thenReturn(NotificationChannel.Result.accepted("smsprovider", "ref1"));

    NotificationLog log =
        service.sendTemplated(Channel.SMS, "EMI_DUE", "9876543210", Map.of("name", "Rahul"), null);

    assertThat(log.getStatus()).isEqualTo("SENT");
    assertThat(log.getAttempts()).isEqualTo(1);
    verify(logs).save(any());
  }

  @Test
  void sendTemplatedRetriesThenFails() {
    when(channel.send(any(), any(), any()))
        .thenReturn(NotificationChannel.Result.rejected("smsprovider", "down"));

    NotificationLog log =
        service.sendTemplated(Channel.SMS, "EMI_DUE", "9876543210", Map.of(), null);

    assertThat(log.getStatus()).isEqualTo("FAILED");
    assertThat(log.getAttempts()).isEqualTo(3); // 1 + 2 retries
    verify(channel, times(3)).send(any(), any(), any());
    verify(outbox, atLeast(1)).append(any()); // NOTIFICATION_FAILED
  }

  @Test
  void scheduleRemindersCreatesD3D1Dday() {
    when(reminders.existsByTenantIdAndSourceRefAndEmiNumberAndOffsetDays(
            any(), any(), any(), org.mockito.ArgumentMatchers.anyInt()))
        .thenReturn(false);

    int created =
        service.scheduleReminders(
            "INSTALLMENT_EMI",
            "inst1",
            1,
            Channel.SMS,
            "EMI_DUE",
            "9876543210",
            Map.of("name", "Rahul"),
            LocalDate.of(2026, 7, 20),
            null,
            LocalDate.of(2026, 7, 1));

    assertThat(created).isEqualTo(3);
    verify(reminders, times(3)).save(any(ReminderSchedule.class));
  }

  @Test
  void processDueRemindersSendsAndMarks() {
    ReminderSchedule due =
        new ReminderSchedule(
            "r1",
            "tenant1",
            "INSTALLMENT_EMI",
            "inst1",
            1,
            Channel.SMS,
            "EMI_DUE",
            "9876543210",
            "{\"name\":\"Rahul\"}",
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 1),
            0,
            clock.instant());
    when(reminders.findByStatusAndSendOnLessThanEqualOrderBySendOn(
            org.mockito.ArgumentMatchers.eq("SCHEDULED"), any(), any()))
        .thenReturn(List.of(due));
    when(channel.send(any(), any(), any()))
        .thenReturn(NotificationChannel.Result.accepted("smsprovider", "ref9"));

    int sent = service.processDueReminders(LocalDate.of(2026, 7, 1), 50);

    assertThat(sent).isEqualTo(1);
    assertThat(due.getStatus()).isEqualTo("SENT");
    verify(outbox, atLeast(1)).append(any()); // REMINDER_SENT
  }

  @Test
  void recordDeliveryMarksDelivered() {
    NotificationLog log =
        new NotificationLog(
            "l1",
            "tenant1",
            Channel.SMS,
            "9876543210",
            "EMI_DUE",
            null,
            "body",
            null,
            clock.instant());
    log.markSent("smsprovider", "ref1", clock.instant());
    when(logs.findByProviderAndProviderRef("smsprovider", "ref1")).thenReturn(Optional.of(log));

    NotificationLog updated = service.recordDelivery("smsprovider", "ref1", true, null);

    assertThat(updated.getStatus()).isEqualTo("DELIVERED");
    verify(outbox, atLeast(1)).append(any()); // NOTIFICATION_DELIVERED
  }
}
