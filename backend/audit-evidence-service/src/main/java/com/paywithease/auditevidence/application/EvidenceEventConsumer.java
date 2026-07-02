package com.paywithease.auditevidence.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.auditevidence.domain.EvidenceIntegrity;
import com.paywithease.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Records immutable system evidence from upstream domain events. Ledger postings arrive on {@code
 * ledger.events} and completed vendor payments on {@code payout.events}. Each mapped event is
 * turned into an evidence item whose SHA-256 hash is computed from a stable canonical string of the
 * payload's key fields, so the recorded proof is independently re-verifiable.
 *
 * <p>{@link AuditEvidenceService#recordSystemEvidence} is idempotent per (entity, contentHash), so
 * at-least-once redeliveries are safe — no separate processed-events table is needed; the natural
 * key provides dedupe. Evidence for a posting is recorded for every journal entry, including
 * reversal postings, so the proof of a reversed transaction survives the reversal.
 *
 * <p>Handling is fail-soft: a bad message is logged rather than rethrown so one poison record
 * cannot wedge the partition; a real implementation would dead-letter it. No sensitive amounts are
 * logged.
 */
@Component
public class EvidenceEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(EvidenceEventConsumer.class);
  private static final String ACTOR = "consumer:audit-evidence";

  private final AuditEvidenceService service;
  private final ObjectMapper objectMapper;

  public EvidenceEventConsumer(AuditEvidenceService service, ObjectMapper objectMapper) {
    this.service = service;
    this.objectMapper = objectMapper;
  }

  @KafkaListener(
      topics = {"ledger.events", "payout.events"},
      groupId = "audit-evidence-service")
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
      log.error("Failed to handle audit-evidence event: {}", e.getMessage(), e);
    }
  }

  private void handle(String eventType, JsonNode payload) {
    switch (eventType) {
      case "JOURNAL_ENTRY_POSTED" -> {
        // accounting-ledger-service LedgerPostingService payload: journalEntryId, amountMinor,
        // entryDate. Recorded for every posting, including reversals.
        String journalEntryId = payload.path("journalEntryId").asText(null);
        if (journalEntryId == null) {
          log.debug("Skipping JOURNAL_ENTRY_POSTED with no journalEntryId");
          return;
        }
        long amountMinor = payload.path("amountMinor").asLong(0);
        String entryDate =
            payload.hasNonNull("entryDate") ? payload.path("entryDate").asText() : "";
        String canonical = journalEntryId + "|" + amountMinor + "|" + entryDate;
        service.recordSystemEvidence(
            "JOURNAL_ENTRY",
            journalEntryId,
            EvidenceIntegrity.sha256Hex(canonical),
            "Journal entry posted");
      }
      case "VENDOR_PAYMENT_COMPLETED" -> {
        // payout-service PayoutService payload: payoutId, partyId, amountMinor.
        String payoutId = payload.path("payoutId").asText(null);
        if (payoutId == null) {
          log.debug("Skipping VENDOR_PAYMENT_COMPLETED with no payoutId");
          return;
        }
        long amountMinor = payload.path("amountMinor").asLong(0);
        String partyId = payload.hasNonNull("partyId") ? payload.path("partyId").asText() : "";
        String canonical = payoutId + "|" + amountMinor + "|" + partyId;
        service.recordSystemEvidence(
            "PAYOUT", payoutId, EvidenceIntegrity.sha256Hex(canonical), "Vendor payment completed");
      }
      default -> log.debug("Ignoring unhandled event type {}", eventType);
    }
  }
}
