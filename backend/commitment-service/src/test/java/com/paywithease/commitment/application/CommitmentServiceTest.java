package com.paywithease.commitment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.commitment.domain.Commitment;
import com.paywithease.commitment.domain.CommitmentEvent;
import com.paywithease.commitment.domain.CounterpartyType;
import com.paywithease.commitment.domain.SourceType;
import com.paywithease.commitment.infrastructure.CommitmentEventRepository;
import com.paywithease.commitment.infrastructure.CommitmentRepository;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.idempotency.IdempotencyService;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
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
class CommitmentServiceTest {

  @Mock CommitmentRepository commitments;
  @Mock CommitmentEventRepository events;
  @Mock IdempotencyService idempotency;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private CommitmentService service;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-02T00:00:00Z"), ZoneOffset.UTC);
  private static final LocalDate DUE = LocalDate.of(2026, 7, 10);

  @BeforeEach
  void setUp() {
    service =
        new CommitmentService(commitments, events, idempotency, audit, outbox, objectMapper, clock);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
    when(commitments.save(any())).thenAnswer(returnsFirstArg());
    when(events.save(any())).thenAnswer(returnsFirstArg());
    when(idempotency.hashRequest(any())).thenReturn("hash");
    when(idempotency.execute(anyString(), any(), anyString(), eq("hash"), any(), any()))
        .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(5)).get());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  private CommitmentService.CreateCommand createCmd(String sourceRef) {
    return new CommitmentService.CreateCommand(
        "CUSTOMER", "cust1", "Raj", "MANUAL", sourceRef, "Course fee promise", 100_000, DUE);
  }

  private Commitment commitment(LocalDate dueDate) {
    return new Commitment(
        "c1",
        "tenant1",
        CounterpartyType.CUSTOMER,
        "cust1",
        "Raj",
        SourceType.MANUAL,
        "src1",
        "Course fee",
        100_000,
        dueDate,
        "actor1",
        clock.instant());
  }

  @Test
  void createPersistsAuditAndEvent() {
    CommitmentService.CommitmentResult result = service.create("idem1", createCmd("src1"));

    assertThat(result.amountMinor()).isEqualTo(100_000);
    assertThat(result.status()).isEqualTo("PROMISED");
    verify(commitments).save(any(Commitment.class));
    verify(events).save(any(CommitmentEvent.class));
    verify(audit).record(eq("COMMITMENT_CREATED"), eq("commitment"), any(), any());
    verify(outbox).append(any(EventEnvelope.class));
  }

  @Test
  void duplicateSourceIsRejected() {
    when(commitments.existsByTenantIdAndSourceTypeAndSourceRef("tenant1", "MANUAL", "src1"))
        .thenReturn(true);

    assertThatThrownBy(() -> service.create("idem1", createCmd("src1")))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void paymentCanPartiallyCloseCommitment() {
    Commitment c = commitment(DUE);
    when(commitments.findByTenantIdAndId("tenant1", "c1")).thenReturn(Optional.of(c));

    CommitmentService.CommitmentResult result =
        service.recordPayment("idem2", "c1", new CommitmentService.PaymentCommand(40_000, "UPI"));

    assertThat(result.paidMinor()).isEqualTo(40_000);
    assertThat(result.outstandingMinor()).isEqualTo(60_000);
    assertThat(result.status()).isEqualTo("PARTIALLY_PAID");
    verify(audit).record(eq("COMMITMENT_PARTIALLY_PAID"), eq("commitment"), eq("c1"), any());
  }

  @Test
  void rescheduleAuditsOldAndNewDueDates() {
    Commitment c = commitment(DUE);
    when(commitments.findByTenantIdAndId("tenant1", "c1")).thenReturn(Optional.of(c));

    CommitmentService.CommitmentResult result =
        service.reschedule(
            "c1", new CommitmentService.RescheduleCommand(DUE.plusDays(3), "Friday follow-up"));

    assertThat(result.dueDate()).isEqualTo(DUE.plusDays(3));
    verify(audit).record(eq("COMMITMENT_RESCHEDULED"), eq("commitment"), eq("c1"), any());
  }

  @Test
  void markOverdueBrokenOnlyChangesOpenOverdue() {
    Commitment c = commitment(LocalDate.of(2026, 7, 1));
    when(commitments.overdue("tenant1", LocalDate.of(2026, 7, 2))).thenReturn(List.of(c));

    int changed = service.markOverdueBroken();

    assertThat(changed).isEqualTo(1);
    assertThat(c.getStatus()).isEqualTo("BROKEN");
    verify(outbox).append(any(EventEnvelope.class));
  }

  @Test
  void getBlocksCrossTenantByTenantScopedLookup() {
    when(commitments.findByTenantIdAndId("tenant1", "missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.get("missing"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void listDelegatesTenantScopedFilters() {
    when(commitments.list("tenant1", "BROKEN", "CUSTOMER"))
        .thenReturn(List.of(commitment(LocalDate.of(2026, 7, 1))));

    List<CommitmentService.CommitmentResult> rows = service.list("BROKEN", "CUSTOMER");

    assertThat(rows).hasSize(1);
    verify(commitments, times(1)).list("tenant1", "BROKEN", "CUSTOMER");
  }
}
