package com.paywithease.notification.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.notification.domain.Channel;
import com.paywithease.notification.domain.NotificationLog;
import com.paywithease.notification.domain.NotificationTemplate;
import com.paywithease.notification.domain.ReminderPlanner;
import com.paywithease.notification.domain.ReminderSchedule;
import com.paywithease.notification.domain.TemplateEngine;
import com.paywithease.notification.infrastructure.NotificationLogRepository;
import com.paywithease.notification.infrastructure.NotificationTemplateRepository;
import com.paywithease.notification.infrastructure.ReminderScheduleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renders + sends notifications (with retry), records delivery status, and drives D-3/D-1/D-day
 * reminders. A send is only marked SENT when a provider accepts it and DELIVERED only on a provider
 * receipt — the engine never claims delivery without acknowledgement.
 */
@Service
public class NotificationService {

  private static final String SOURCE = "notification-service";

  private final NotificationTemplateRepository templates;
  private final NotificationLogRepository logs;
  private final ReminderScheduleRepository reminders;
  private final ChannelRouter router;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;
  private final int maxRetries;

  public NotificationService(
      NotificationTemplateRepository templates,
      NotificationLogRepository logs,
      ReminderScheduleRepository reminders,
      ChannelRouter router,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock,
      @Value("${peb.notification.max-retries:2}") int maxRetries) {
    this.templates = templates;
    this.logs = logs;
    this.reminders = reminders;
    this.router = router;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
    this.maxRetries = maxRetries;
  }

  @Transactional
  public NotificationLog sendTemplated(
      Channel channel,
      String templateCode,
      String recipient,
      Map<String, String> vars,
      String reminderId) {
    String tenantId = TenantContext.requireTenantId();
    NotificationTemplate template =
        templates
            .findByTenantIdAndCodeAndChannel(tenantId, templateCode, channel.name())
            .filter(NotificationTemplate::isActive)
            .orElseThrow(
                () -> ApiException.notFound("Active template " + templateCode + "/" + channel));

    String subject = TemplateEngine.render(template.getSubject(), vars);
    String body = TemplateEngine.render(template.getBody(), vars);

    NotificationLog log =
        new NotificationLog(
            Ulid.newId(),
            tenantId,
            channel,
            recipient,
            templateCode,
            subject,
            body,
            reminderId,
            clock.instant());

    NotificationChannel provider = router.forChannel(channel);
    NotificationChannel.Result result = null;
    for (int attempt = 0; attempt <= maxRetries; attempt++) {
      log.recordAttempt();
      result = provider.send(recipient, subject, body);
      if (result.accepted()) {
        break;
      }
    }

    Instant now = clock.instant();
    if (result != null && result.accepted()) {
      log.markSent(result.provider(), result.providerRef(), now);
    } else {
      String reason = result == null ? "no provider result" : result.failureReason();
      log.markFailed(result == null ? null : result.provider(), reason, now);
      emit(
          "NOTIFICATION_FAILED",
          tenantId,
          log.getId(),
          Map.of("channel", channel.name(), "reason", reason));
    }
    logs.save(log);
    audit.record(
        "NOTIFICATION_" + log.getStatus(),
        "notification",
        log.getId(),
        Map.of("channel", channel.name(), "attempts", log.getAttempts()));
    return log;
  }

  @Transactional
  public int scheduleReminders(
      String sourceType,
      String sourceRef,
      Integer emiNumber,
      Channel channel,
      String templateCode,
      String recipient,
      Map<String, String> vars,
      LocalDate dueDate,
      List<Integer> offsets,
      LocalDate today) {
    String tenantId = TenantContext.requireTenantId();
    String varsJson = writeJson(vars);
    int created = 0;
    for (ReminderPlanner.PlannedReminder p :
        ReminderPlanner.plan(
            dueDate, offsets == null ? ReminderPlanner.DEFAULT_OFFSETS : offsets, today)) {
      if (sourceRef != null
          && reminders.existsByTenantIdAndSourceRefAndEmiNumberAndOffsetDays(
              tenantId, sourceRef, emiNumber, p.offsetDays())) {
        continue; // dedupe
      }
      reminders.save(
          new ReminderSchedule(
              Ulid.newId(),
              tenantId,
              sourceType,
              sourceRef,
              emiNumber,
              channel,
              templateCode,
              recipient,
              varsJson,
              dueDate,
              p.sendOn(),
              p.offsetDays(),
              clock.instant()));
      created++;
    }
    return created;
  }

  /** Fires all reminders whose send date has arrived. Resolves the tenant per reminder row. */
  @Transactional
  public int processDueReminders(LocalDate today, int batchSize) {
    List<ReminderSchedule> due =
        reminders.findByStatusAndSendOnLessThanEqualOrderBySendOn(
            "SCHEDULED", today, Limit.of(batchSize));
    int sent = 0;
    for (ReminderSchedule reminder : due) {
      var previous = TenantContext.current().orElse(null);
      TenantContext.set(
          new TenantContext.Principal(
              reminder.getTenantId(), reminder.getTenantId(), "scheduler:notification", null));
      try {
        sendTemplated(
            reminder.channelEnum(),
            reminder.getTemplateCode(),
            reminder.getRecipient(),
            readJson(reminder.getVariables()),
            reminder.getId());
        reminder.markSent(clock.instant());
        reminders.save(reminder);
        emit(
            "REMINDER_SENT",
            reminder.getTenantId(),
            reminder.getId(),
            Map.of(
                "offsetDays", reminder.getOffsetDays(), "channel", reminder.channelEnum().name()));
        sent++;
      } finally {
        if (previous != null) {
          TenantContext.set(previous);
        } else {
          TenantContext.clear();
        }
      }
    }
    return sent;
  }

  /** Applies a provider delivery receipt: DELIVERED (with ack) or FAILED. */
  @Transactional
  public NotificationLog recordDelivery(
      String provider, String providerRef, boolean delivered, String reason) {
    NotificationLog log =
        logs.findByProviderAndProviderRef(provider, providerRef)
            .orElseThrow(() -> ApiException.notFound("Notification for provider ref"));
    Instant now = clock.instant();
    if (delivered) {
      log.markDelivered(now);
      emit("NOTIFICATION_DELIVERED", log.getTenantId(), log.getId(), Map.of("provider", provider));
    } else {
      log.markDeliveryFailed(reason, now);
      emit(
          "NOTIFICATION_FAILED",
          log.getTenantId(),
          log.getId(),
          Map.of("provider", provider, "reason", reason == null ? "" : reason));
    }
    logs.save(log);
    return log;
  }

  private void emit(String eventType, String tenantId, String aggregateId, Map<String, ?> data) {
    ObjectNode payload = objectMapper.valueToTree(data);
    payload.put("id", aggregateId);
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(tenantId)
            .businessId(tenantId)
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(aggregateId)
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }

  private String writeJson(Map<String, String> vars) {
    try {
      return objectMapper.writeValueAsString(vars == null ? Map.of() : vars);
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to serialize reminder variables", e);
    }
  }

  private Map<String, String> readJson(String json) {
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
    } catch (Exception e) {
      return Map.of();
    }
  }
}
