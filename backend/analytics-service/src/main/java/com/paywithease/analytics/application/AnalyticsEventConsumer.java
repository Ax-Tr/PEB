package com.paywithease.analytics.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.tenant.TenantContext;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Projects upstream domain events into the analytics read-model. Invoices arrive on {@code
 * invoice.events}, purchases/expenses on {@code purchase.events}, collections on {@code
 * payment.events}, and vendor payouts on {@code payout.events}. The ingest methods on {@link
 * AnalyticsService} are idempotent per aggregate natural key, so at-least-once redeliveries are
 * safe (no separate processed-events table is needed — the natural key provides dedupe, mirroring
 * reconciliation-service). After a successful projection the stream watermark is advanced with the
 * envelope's event id so {@code /freshness} can report staleness.
 *
 * <p>Handling is fail-soft: a bad message is logged rather than rethrown so one poison record
 * cannot wedge the partition; a real implementation would dead-letter it. No sensitive amounts are
 * logged.
 *
 * <p>NOTE: the wire envelope carries no occurredAt/business date for these events, so periods are
 * derived from ingest-time (Asia/Kolkata). A future event-schema enrichment should carry the real
 * business date.
 */
@Component
public class AnalyticsEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(AnalyticsEventConsumer.class);
  private static final String ACTOR = "consumer:analytics";
  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

  private final AnalyticsService service;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public AnalyticsEventConsumer(AnalyticsService service, ObjectMapper objectMapper, Clock clock) {
    this.service = service;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @KafkaListener(
      topics = {
        "invoice.events",
        "payment.events",
        "purchase.events",
        "payout.events",
        "commitment.events"
      },
      groupId = "analytics-service")
  public void onEvent(String message) {
    try {
      JsonNode root = objectMapper.readTree(message);
      String eventType = root.path("eventType").asText("");
      String tenantId = root.path("tenantId").asText(null);
      JsonNode payload = root.path("payload");
      JsonNode headers = root.path("headers");
      String correlationId = headers.path("correlationId").asText(null);
      String eventId = headers.path("eventId").asText(null);

      if (tenantId == null || tenantId.isBlank()) {
        log.warn("Ignoring event with no tenantId: type={}", eventType);
        return;
      }

      TenantContext.set(new TenantContext.Principal(tenantId, tenantId, ACTOR, correlationId));
      try {
        handle(eventType, payload, eventId);
      } finally {
        TenantContext.clear();
      }
    } catch (Exception e) {
      // Fail-soft: log and swallow so a single bad message does not block the partition.
      log.error("Failed to handle analytics event: {}", e.getMessage(), e);
    }
  }

  private void handle(String eventType, JsonNode payload, String eventId) {
    // NOTE: period is ingest-time (Asia/Kolkata) because upstream events carry no business date.
    LocalDate istDate = LocalDate.ofInstant(clock.instant(), IST);

    switch (eventType) {
      case "INVOICE_GENERATED" -> {
        String invoiceId = text(payload, "invoiceId");
        if (invoiceId == null) {
          log.debug("Skipping INVOICE_GENERATED with no invoiceId");
          return;
        }
        String invoiceNumber = text(payload, "invoiceNumber");
        String customerId = text(payload, "customerId");
        String supplyType = text(payload, "supplyType");
        long totalMinor = payload.path("totalAmountMinor").asLong(0);
        long taxMinor = payload.path("totalTaxMinor").asLong(0);
        service.ingestInvoice(
            invoiceId, invoiceNumber, customerId, supplyType, totalMinor, taxMinor, istDate);
        service.advanceWatermark("invoice.events", eventId);
      }
      case "PURCHASE_BILL_CREATED" -> {
        String purchaseBillId = text(payload, "purchaseBillId");
        if (purchaseBillId == null) {
          log.debug("Skipping PURCHASE_BILL_CREATED with no purchaseBillId");
          return;
        }
        String vendorId = text(payload, "vendorId");
        long netMinor = payload.path("netMinor").asLong(0);
        long inputGstMinor = payload.path("inputGstMinor").asLong(0);
        long totalMinor = payload.path("totalAmountMinor").asLong(0);
        service.ingestPurchase(
            purchaseBillId, vendorId, netMinor, inputGstMinor, totalMinor, istDate);
        service.advanceWatermark("purchase.events", eventId);
      }
      case "EXPENSE_APPROVED" -> {
        String expenseId = text(payload, "expenseId");
        if (expenseId == null) {
          log.debug("Skipping EXPENSE_APPROVED with no expenseId");
          return;
        }
        String category = text(payload, "category");
        long amountMinor = payload.path("amountMinor").asLong(0);
        service.ingestExpense(expenseId, category, amountMinor, istDate);
        service.advanceWatermark("purchase.events", eventId);
      }
      case "PAYMENT_RECEIVED" -> {
        String paymentRequestId = text(payload, "paymentRequestId");
        long appliedMinor = payload.path("appliedMinor").asLong(0);
        if (paymentRequestId == null || appliedMinor == 0) {
          log.debug("Skipping PAYMENT_RECEIVED with no paymentRequestId or zero appliedMinor");
          return;
        }
        // counterparty (customer) is not carried on this event → null.
        service.ingestCashMovement(
            paymentRequestId, "INFLOW", "PAYMENT", null, appliedMinor, istDate);
        service.advanceWatermark("payment.events", eventId);
      }
      case "VENDOR_PAYMENT_COMPLETED" -> {
        String payoutId = text(payload, "payoutId");
        long amountMinor = payload.path("amountMinor").asLong(0);
        if (payoutId == null || amountMinor == 0) {
          log.debug("Skipping VENDOR_PAYMENT_COMPLETED with no payoutId or zero amountMinor");
          return;
        }
        String partyId = text(payload, "partyId");
        service.ingestCashMovement(payoutId, "OUTFLOW", "PAYOUT", partyId, amountMinor, istDate);
        service.advanceWatermark("payout.events", eventId);
      }
      case "COMMITMENT_CREATED",
          "COMMITMENT_PARTIALLY_PAID",
          "COMMITMENT_PAID",
          "COMMITMENT_RESCHEDULED",
          "COMMITMENT_BROKEN",
          "COMMITMENT_CANCELLED" -> {
        String commitmentId = text(payload, "commitmentId");
        String dueDate = text(payload, "dueDate");
        if (commitmentId == null || dueDate == null) {
          log.debug("Skipping {} with no commitmentId or dueDate", eventType);
          return;
        }
        service.ingestCommitment(
            commitmentId,
            blankTo(payload.path("counterpartyType").asText(null), "OTHER"),
            text(payload, "counterpartyId"),
            text(payload, "counterpartyName"),
            text(payload, "sourceType"),
            LocalDate.parse(dueDate),
            payload.path("amountMinor").asLong(0),
            payload.path("paidMinor").asLong(0),
            payload.path("outstandingMinor").asLong(0),
            blankTo(payload.path("status").asText(null), "PROMISED"));
        service.advanceWatermark("commitment.events", eventId);
      }
      default -> log.debug("Ignoring unhandled event type {}", eventType);
    }
  }

  /** Null-safe string read: returns null unless the field is present and non-null. */
  private static String text(JsonNode payload, String field) {
    return payload.hasNonNull(field) ? payload.path(field).asText() : null;
  }

  private static String blankTo(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
