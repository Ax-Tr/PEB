package com.paywithease.ledger.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.ledger.domain.FinancialPeriod;
import com.paywithease.ledger.domain.MonthLock;
import com.paywithease.ledger.infrastructure.FinancialPeriodRepository;
import com.paywithease.ledger.infrastructure.MonthLockRepository;
import java.time.Clock;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Month-lock lifecycle. Locking blocks further posting into the period; reopening is a
 * maker-checker action (Sprint 5 records the request + reason and emits MONTH_REOPEN_REQUESTED; the
 * approval workflow is completed in the CA/collaboration sprint). Every action is logged
 * append-only.
 */
@Service
public class MonthLockService {

  private static final String SOURCE = "accounting-ledger-service";

  private final FinancialPeriodRepository periods;
  private final MonthLockRepository lockLog;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public MonthLockService(
      FinancialPeriodRepository periods,
      MonthLockRepository lockLog,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock) {
    this.periods = periods;
    this.lockLog = lockLog;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Transactional
  public FinancialPeriod lock(int year, int month, String reason, String actorId) {
    String tenantId = TenantContext.requireTenantId();
    FinancialPeriod period = getOrCreate(tenantId, year, month);
    period.lock(clock.instant());
    periods.save(period);
    log(tenantId, period.getId(), "LOCK", reason, actorId);
    audit.record(
        "MONTH_LOCKED", "financial_period", period.getId(), Map.of("year", year, "month", month));
    emit("MONTH_LOCKED", period);
    return period;
  }

  @Transactional
  public FinancialPeriod reopen(int year, int month, String reason, String actorId) {
    String tenantId = TenantContext.requireTenantId();
    FinancialPeriod period = getOrCreate(tenantId, year, month);
    period.reopen();
    periods.save(period);
    log(tenantId, period.getId(), "REOPEN", reason, actorId);
    audit.record(
        "MONTH_REOPEN_REQUESTED",
        "financial_period",
        period.getId(),
        Map.of("year", year, "month", month, "reason", reason == null ? "" : reason));
    emit("MONTH_REOPEN_REQUESTED", period);
    return period;
  }

  @Transactional(readOnly = true)
  public FinancialPeriod status(int year, int month) {
    return periods
        .findByTenantIdAndYearAndMonth(TenantContext.requireTenantId(), year, month)
        .orElseGet(
            () ->
                new FinancialPeriod(
                    Ulid.newId(), TenantContext.requireTenantId(), year, month, clock.instant()));
  }

  private FinancialPeriod getOrCreate(String tenantId, int year, int month) {
    return periods
        .findByTenantIdAndYearAndMonth(tenantId, year, month)
        .orElseGet(
            () ->
                periods.save(
                    new FinancialPeriod(Ulid.newId(), tenantId, year, month, clock.instant())));
  }

  private void log(String tenantId, String periodId, String action, String reason, String actorId) {
    lockLog.save(
        new MonthLock(Ulid.newId(), tenantId, periodId, action, reason, actorId, clock.instant()));
  }

  private void emit(String eventType, FinancialPeriod period) {
    var payload =
        objectMapper
            .createObjectNode()
            .put("periodId", period.getId())
            .put("year", period.getYear())
            .put("month", period.getMonth())
            .put("state", period.getState().name());
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(period.getId() == null ? null : TenantContext.requireTenantId())
            .businessId(TenantContext.requireTenantId())
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(period.getId())
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }
}
