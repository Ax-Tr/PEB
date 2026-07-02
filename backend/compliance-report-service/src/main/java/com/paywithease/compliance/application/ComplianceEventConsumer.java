package com.paywithease.compliance.application;

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
 * Ingests period source rows from upstream domain events so compliance reports can be prepared.
 * Sales rows arrive on {@code invoice.events}, purchase/ITC rows on {@code purchase.events}, and
 * payroll/TDS rows on {@code payroll.events}. {@link ComplianceReportService#recordSource} is
 * idempotent per (recordType, sourceRef), so at-least-once redeliveries are safe (no separate
 * processed-events table is needed — the natural key provides dedupe).
 *
 * <p>Handling is fail-soft: a bad message is logged rather than rethrown so one poison record
 * cannot wedge the partition; a real implementation would dead-letter it. No sensitive amounts are
 * logged.
 *
 * <p>NOTE: the invoice/purchase events do not currently carry the document/period date, and the
 * wire envelope carries no occurredAt, so ingest-time (Asia/Kolkata) is used to derive year/month
 * for those two. The payroll event carries its own year/month and is used directly. A future
 * event-schema enrichment should carry the real period date.
 */
@Component
public class ComplianceEventConsumer {

  private static final Logger log = LoggerFactory.getLogger(ComplianceEventConsumer.class);
  private static final String ACTOR = "consumer:compliance";
  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

  private final ComplianceReportService service;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public ComplianceEventConsumer(
      ComplianceReportService service, ObjectMapper objectMapper, Clock clock) {
    this.service = service;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @KafkaListener(
      topics = {"invoice.events", "purchase.events", "payroll.events"},
      groupId = "compliance-report-service")
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
      log.error("Failed to handle compliance event: {}", e.getMessage(), e);
    }
  }

  private void handle(String eventType, JsonNode payload) {
    // Ingest-time period fallback (Asia/Kolkata) for events without a period date — see class note.
    LocalDate today = LocalDate.ofInstant(clock.instant(), IST);

    switch (eventType) {
      case "INVOICE_GENERATED" -> {
        // invoice-gst-service InvoiceService payload: invoiceId, invoiceNumber, customerId,
        // totalAmountMinor, totalTaxMinor, supplyType. taxable = total - tax.
        long totalAmountMinor = payload.path("totalAmountMinor").asLong(0);
        long taxMinor = payload.path("totalTaxMinor").asLong(0);
        long taxableMinor = totalAmountMinor - taxMinor;
        String sourceRef = payload.path("invoiceId").asText(null);
        if (sourceRef == null) {
          log.debug("Skipping INVOICE_GENERATED with no invoiceId");
          return;
        }
        // supplyType is optional (B2B/B2C/etc.); null-safe.
        String supplyType =
            payload.hasNonNull("supplyType") ? payload.path("supplyType").asText() : null;
        String reference =
            payload.hasNonNull("invoiceNumber") ? payload.path("invoiceNumber").asText() : null;
        service.recordSource(
            new ComplianceReportService.SourceCommand(
                "SALES",
                today.getYear(),
                today.getMonthValue(),
                taxableMinor,
                taxMinor,
                0L,
                0L,
                supplyType,
                sourceRef,
                reference));
      }
      case "PURCHASE_BILL_CREATED" -> {
        // purchase-expense-service PurchaseService payload: purchaseBillId, vendorId, netMinor,
        // inputGstMinor, totalAmountMinor, reverseCharge.
        String sourceRef = payload.path("purchaseBillId").asText(null);
        if (sourceRef == null) {
          log.debug("Skipping PURCHASE_BILL_CREATED with no purchaseBillId");
          return;
        }
        long netMinor = payload.path("netMinor").asLong(0);
        long inputGstMinor = payload.path("inputGstMinor").asLong(0);
        service.recordSource(
            new ComplianceReportService.SourceCommand(
                "PURCHASE",
                today.getYear(),
                today.getMonthValue(),
                netMinor,
                inputGstMinor,
                0L,
                0L,
                null,
                sourceRef,
                null));
      }
      case "SALARY_RUN_CREATED" -> {
        // employee-payroll-service SalaryRunService payload: salaryRunId, year, month,
        // totalEarningsMinor, totalNetMinor, totalStatutoryMinor, totalTdsMinor. Period is present.
        String sourceRef = payload.path("salaryRunId").asText(null);
        if (sourceRef == null) {
          log.debug("Skipping SALARY_RUN_CREATED with no salaryRunId");
          return;
        }
        int year = payload.path("year").asInt(today.getYear());
        int month = payload.path("month").asInt(today.getMonthValue());
        long statutoryMinor = payload.path("totalStatutoryMinor").asLong(0);
        long tdsMinor = payload.path("totalTdsMinor").asLong(0);
        service.recordSource(
            new ComplianceReportService.SourceCommand(
                "PAYROLL", year, month, 0L, 0L, statutoryMinor, tdsMinor, null, sourceRef, null));
      }
      default -> log.debug("Ignoring unhandled event type {}", eventType);
    }
  }
}
