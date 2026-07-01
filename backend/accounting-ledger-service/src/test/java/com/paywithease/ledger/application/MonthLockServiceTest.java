package com.paywithease.ledger.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.ledger.domain.FinancialPeriod;
import com.paywithease.ledger.domain.PeriodState;
import com.paywithease.ledger.infrastructure.FinancialPeriodRepository;
import com.paywithease.ledger.infrastructure.MonthLockRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MonthLockServiceTest {

  @Mock FinancialPeriodRepository periods;
  @Mock MonthLockRepository lockLog;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private MonthLockService service;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-05-15T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service = new MonthLockService(periods, lockLog, audit, outbox, objectMapper, clock);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
    when(periods.save(any())).thenAnswer(returnsFirstArg());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  private FinancialPeriod openPeriod() {
    return new FinancialPeriod("per1", "tenant1", 2026, 5, clock.instant());
  }

  @Test
  void lockCreatesAndLocksPeriodAndEmits() {
    when(periods.findByTenantIdAndYearAndMonth("tenant1", 2026, 5)).thenReturn(Optional.empty());

    FinancialPeriod result = service.lock(2026, 5, "May close", "actor1");

    assertThat(result.getState()).isEqualTo(PeriodState.LOCKED);
    ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
    verify(outbox).append(captor.capture());
    assertThat(captor.getValue().eventType()).isEqualTo("MONTH_LOCKED");
    verify(audit).record(eq("MONTH_LOCKED"), any(), any(), any());
  }

  @Test
  void reopenSetsOpenAndRequestsApproval() {
    FinancialPeriod locked = openPeriod();
    locked.lock(clock.instant());
    when(periods.findByTenantIdAndYearAndMonth("tenant1", 2026, 5)).thenReturn(Optional.of(locked));

    FinancialPeriod result = service.reopen(2026, 5, "correction", "actor1");

    assertThat(result.getState()).isEqualTo(PeriodState.OPEN);
    ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
    verify(outbox).append(captor.capture());
    assertThat(captor.getValue().eventType()).isEqualTo("MONTH_REOPEN_REQUESTED");
  }

  @Test
  void lockAlreadyLockedThrows() {
    FinancialPeriod locked = openPeriod();
    locked.lock(clock.instant());
    when(periods.findByTenantIdAndYearAndMonth("tenant1", 2026, 5)).thenReturn(Optional.of(locked));

    assertThatThrownBy(() -> service.lock(2026, 5, "again", "actor1"))
        .isInstanceOf(ApiException.class);
  }
}
