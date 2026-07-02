package com.paywithease.ledger.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.ledger.domain.Accounts;
import com.paywithease.ledger.domain.JournalCommand;
import com.paywithease.ledger.domain.PostingTemplates;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Posts to the ledger from upstream domain events. Each source event id is used as the journal's
 * idempotency key, so redeliveries are safe. Handling is deliberately fail-soft in Sprint 5 — a bad
 * message is logged rather than rethrown so one poison record cannot wedge the partition; a real
 * implementation would dead-letter it.
 */
@Component
public class LedgerEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(LedgerEventConsumer.class);
  private static final String ACTOR = "consumer:accounting-ledger";
  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

  private final CoaSeeder coaSeeder;
  private final LedgerPostingService postingService;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public LedgerEventConsumer(
      CoaSeeder coaSeeder,
      LedgerPostingService postingService,
      ObjectMapper objectMapper,
      Clock clock) {
    this.coaSeeder = coaSeeder;
    this.postingService = postingService;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @KafkaListener(
      topics = {
        "tenant.events",
        "invoice.events",
        "payment.events",
        "purchase.events",
        "payout.events",
        "payroll.events"
      },
      groupId = "accounting-ledger")
  public void onEvent(String message) {
    try {
      JsonNode root = objectMapper.readTree(message);
      String eventType = root.path("eventType").asText("");
      String tenantId = root.path("tenantId").asText(null);
      JsonNode payload = root.path("payload");
      JsonNode headers = root.path("headers");
      String eventId = headers.path("eventId").asText(null);
      String correlationId = headers.path("correlationId").asText(null);

      if (tenantId == null || tenantId.isBlank()) {
        log.warn("Ignoring event with no tenantId: type={}", eventType);
        return;
      }

      TenantContext.set(new TenantContext.Principal(tenantId, tenantId, ACTOR, correlationId));
      try {
        handle(eventType, tenantId, eventId, correlationId, payload);
      } finally {
        TenantContext.clear();
      }
    } catch (Exception e) {
      // Fail-soft: log and swallow so a single bad message does not block the partition.
      log.error("Failed to handle ledger event: {}", e.getMessage(), e);
    }
  }

  private void handle(
      String eventType, String tenantId, String eventId, String correlationId, JsonNode payload) {
    LocalDate entryDate = LocalDate.ofInstant(clock.instant(), IST);
    switch (eventType) {
      case "BUSINESS_CREATED" -> {
        int created = coaSeeder.seedIfAbsent(tenantId);
        log.info("Seeded {} accounts for tenant {} on BUSINESS_CREATED", created, tenantId);
      }
      case "INVOICE_GENERATED" -> {
        long totalAmountMinor = payload.path("totalAmountMinor").asLong(0);
        long totalTaxMinor = payload.path("totalTaxMinor").asLong(0);
        JournalCommand cmd =
            PostingTemplates.customerInvoice(
                entryDate,
                totalAmountMinor,
                totalTaxMinor,
                "invoice-gst-service",
                eventId,
                correlationId,
                "Invoice " + payload.path("invoiceNumber").asText(""));
        postingService.post(cmd, ACTOR);
      }
      case "PAYMENT_RECEIVED" -> {
        long appliedMinor = payload.path("appliedMinor").asLong(0);
        if (appliedMinor > 0) {
          JournalCommand cmd =
              PostingTemplates.customerPayment(
                  entryDate,
                  appliedMinor,
                  Accounts.UPI_CLEARING,
                  "payment-collection-service",
                  eventId,
                  correlationId,
                  "Payment " + payload.path("reference").asText(""));
          postingService.post(cmd, ACTOR);
        }
      }
      case "PURCHASE_BILL_CREATED" -> {
        long netMinor = payload.path("netMinor").asLong(0);
        long inputGstMinor = payload.path("inputGstMinor").asLong(0);
        if (netMinor + inputGstMinor > 0) {
          JournalCommand cmd =
              PostingTemplates.vendorPurchase(
                  entryDate,
                  netMinor,
                  inputGstMinor,
                  "purchase-expense-service",
                  eventId,
                  correlationId,
                  "Purchase " + payload.path("purchaseBillId").asText(""));
          postingService.post(cmd, ACTOR);
        }
      }
      case "VENDOR_PAYMENT_COMPLETED" -> {
        long amountMinor = payload.path("amountMinor").asLong(0);
        if (amountMinor > 0) {
          JournalCommand cmd =
              PostingTemplates.vendorPayment(
                  entryDate,
                  amountMinor,
                  Accounts.BANK,
                  "payout-service",
                  eventId,
                  correlationId,
                  "Vendor payment " + payload.path("payoutId").asText(""));
          postingService.post(cmd, ACTOR);
        }
      }
      case "SALARY_RUN_CREATED" -> {
        long totalEarnings = payload.path("totalEarningsMinor").asLong(0);
        long net = payload.path("totalNetMinor").asLong(0);
        long statutory = payload.path("totalStatutoryMinor").asLong(0);
        long tds = payload.path("totalTdsMinor").asLong(0);
        if (totalEarnings > 0) {
          JournalCommand cmd =
              PostingTemplates.salaryRun(
                  entryDate,
                  totalEarnings,
                  net,
                  statutory,
                  tds,
                  "employee-payroll-service",
                  eventId,
                  correlationId,
                  "Payroll " + payload.path("salaryRunId").asText(""));
          postingService.post(cmd, ACTOR);
        }
      }
      default -> log.debug("Ignoring unhandled event type {}", eventType);
    }
  }
}
