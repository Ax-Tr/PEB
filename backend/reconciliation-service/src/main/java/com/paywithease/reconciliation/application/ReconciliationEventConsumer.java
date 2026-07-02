package com.paywithease.reconciliation.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.reconciliation.domain.ReconSide;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Records reconcilable items from upstream domain events. External (imported bank) rows arrive on
 * {@code ingestion.events}; internal records (payment/payout/invoice) arrive on their own topics.
 * {@link ReconciliationService#recordItem} is idempotent per source ref, so redeliveries are safe.
 * Handling is deliberately fail-soft in Sprint 5 — a bad message is logged rather than rethrown so
 * one poison record cannot wedge the partition; a real implementation would dead-letter it.
 *
 * <p>NOTE: Upstream events currently omit the transaction date; a future event-schema enrichment
 * should carry it so matching uses the real date rather than ingest-time.
 */
@Component
public class ReconciliationEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(ReconciliationEventConsumer.class);
  private static final String ACTOR = "consumer:reconciliation";
  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

  private final ReconciliationService reconciliationService;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public ReconciliationEventConsumer(
      ReconciliationService reconciliationService, ObjectMapper objectMapper, Clock clock) {
    this.reconciliationService = reconciliationService;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @KafkaListener(
      topics = {"ingestion.events", "payment.events", "payout.events", "invoice.events"},
      groupId = "reconciliation")
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
      log.error("Failed to handle reconciliation event: {}", e.getMessage(), e);
    }
  }

  private void handle(String eventType, JsonNode payload) {
    // The import events carry no date field; use today (Asia/Kolkata) as the item date. This is a
    // known limitation — see the class-level note on future event-schema enrichment.
    LocalDate today = LocalDate.ofInstant(clock.instant(), IST);

    switch (eventType) {
      case "BANK_TRANSACTION_IMPORTED" -> {
        // External side: imported bank transaction (source of truth).
        ReconciliationService.ItemCommand cmd =
            new ReconciliationService.ItemCommand(
                "BANK_TXN",
                payload.path("transactionId").asText(null),
                payload.path("direction").asText(null),
                payload.path("amountMinor").asLong(0),
                today,
                payload.path("externalRef").asText(null),
                null,
                payload.path("category").asText(null));
        reconciliationService.recordItem(ReconSide.EXTERNAL, cmd);
      }
      case "PAYMENT_RECEIVED" -> {
        long appliedMinor = payload.path("appliedMinor").asLong(0);
        if (appliedMinor == 0) {
          log.debug("Skipping PAYMENT_RECEIVED with zero appliedMinor");
          return;
        }
        String reference = payload.path("reference").asText(null);
        ReconciliationService.ItemCommand cmd =
            new ReconciliationService.ItemCommand(
                "PAYMENT",
                payload.path("paymentRequestId").asText(null),
                "CREDIT",
                appliedMinor,
                today,
                reference,
                null,
                reference);
        reconciliationService.recordItem(ReconSide.INTERNAL, cmd);
      }
      case "VENDOR_PAYMENT_COMPLETED" -> {
        String payoutId = payload.path("payoutId").asText(null);
        ReconciliationService.ItemCommand cmd =
            new ReconciliationService.ItemCommand(
                "PAYOUT",
                payoutId,
                "DEBIT",
                payload.path("amountMinor").asLong(0),
                today,
                payoutId,
                payload.path("partyId").asText(null),
                "payout");
        reconciliationService.recordItem(ReconSide.INTERNAL, cmd);
      }
      case "INVOICE_GENERATED" -> {
        String invoiceNumber = payload.path("invoiceNumber").asText(null);
        ReconciliationService.ItemCommand cmd =
            new ReconciliationService.ItemCommand(
                "INVOICE",
                payload.path("invoiceId").asText(null),
                "CREDIT",
                payload.path("totalAmountMinor").asLong(0),
                today,
                invoiceNumber,
                payload.path("customerId").asText(null),
                invoiceNumber);
        reconciliationService.recordItem(ReconSide.INTERNAL, cmd);
      }
      default -> log.debug("Ignoring unhandled event type {}", eventType);
    }
  }
}
