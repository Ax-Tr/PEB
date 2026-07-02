package com.paywithease.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to transaction-ingestion events so that recorded transactions could be turned into
 * governed classification suggestions. Wire parsing mirrors the sibling consumers
 * (compliance-report-service / analytics-service): {@code eventType}, top-level {@code tenantId},
 * nested {@code payload}, and {@code headers.correlationId}; {@code TenantContext} is set for the
 * event's tenant and cleared in a finally block. {@link AiAutomationService#classifyTransaction} is
 * idempotent on the (tenant, subjectType, subjectId, kind) natural key, so at-least-once
 * redeliveries are safe (no separate processed-events table is needed). Handling is fail-soft — a
 * bad message is logged, never rethrown, so one poison record cannot wedge the partition. No
 * sensitive fields (narrations, bank details) are ever logged.
 *
 * <p>NOTE: transaction-ingestion-service emits {@code TRANSACTION_CLASSIFIED} and {@code
 * BANK_TRANSACTION_IMPORTED} on {@code ingestion.events}, but the event payload only carries {@code
 * transactionId, source, direction, amountMinor, category, classificationStatus} — it does NOT
 * carry the narration/description text. A meaningful transaction classification is impossible
 * without the narration, so auto-classification is DEFERRED here: this consumer is a no-op that
 * logs and skips every event type. Once the ingestion event schema is enriched to carry the
 * narration, the mapping is a one-liner: {@code service.classifyTransaction(transactionId,
 * narration)} for the "transaction recorded" event. Nothing forces an incorrect mapping in the
 * meantime.
 */
@Component
public class AiEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(AiEventConsumer.class);
  private static final String ACTOR = "consumer:ai-automation";

  private final AiAutomationService service;
  private final ObjectMapper objectMapper;

  public AiEventConsumer(AiAutomationService service, ObjectMapper objectMapper) {
    this.service = service;
    this.objectMapper = objectMapper;
  }

  @KafkaListener(topics = "ingestion.events", groupId = "ai-automation-service")
  public void onEvent(String message) {
    try {
      JsonNode root = objectMapper.readTree(message);
      String eventType = root.path("eventType").asText("");
      String tenantId = root.path("tenantId").asText(null);
      JsonNode payload = root.path("payload");
      JsonNode headers = root.path("headers");
      String correlationId = headers.path("correlationId").asText(null);

      if (tenantId == null || tenantId.isBlank()) {
        log.warn("Ignoring event with no tenantId: type={}", eventType);
        return;
      }

      TenantContext.set(new TenantContext.Principal(tenantId, tenantId, ACTOR, correlationId));
      try {
        handle(eventType, payload);
      } finally {
        TenantContext.clear();
      }
    } catch (Exception e) {
      // Fail-soft: log and swallow so a single bad message does not block the partition.
      log.error("Failed to handle ingestion event: {}", e.getMessage(), e);
    }
  }

  private void handle(String eventType, JsonNode payload) {
    // NOTE: ingestion events carry no narration/description, so auto-classification is deferred.
    // Every event type is intentionally skipped until the upstream schema carries the narration.
    // When it does, map the "transaction recorded" event here via:
    //   String transactionId = text(payload, "transactionId");
    //   String narration = text(payload, "narration");
    //   if (transactionId != null && narration != null) {
    //     service.classifyTransaction(transactionId, narration);
    //   }
    log.debug(
        "Ignoring ingestion event {} (auto-classification deferred: no narration in payload)",
        eventType);
  }

  /** Null-safe string read: returns null unless the field is present and non-null. */
  private static String text(JsonNode payload, String field) {
    return payload.hasNonNull(field) ? payload.path(field).asText() : null;
  }
}
