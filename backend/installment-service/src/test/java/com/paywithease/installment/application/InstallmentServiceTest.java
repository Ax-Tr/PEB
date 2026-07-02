package com.paywithease.installment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.installment.domain.Frequency;
import com.paywithease.installment.domain.Installment;
import com.paywithease.installment.domain.InstallmentEmi;
import com.paywithease.installment.domain.InstallmentType;
import com.paywithease.installment.infrastructure.InstallmentEmiRepository;
import com.paywithease.installment.infrastructure.InstallmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
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
class InstallmentServiceTest {

  @Mock InstallmentRepository installments;
  @Mock InstallmentEmiRepository emis;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private InstallmentService service;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC);
  private static final LocalDate DUE = LocalDate.of(2026, 6, 1);

  @BeforeEach
  void setUp() {
    service = new InstallmentService(installments, emis, audit, outbox, objectMapper, clock);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
    when(installments.save(any())).thenAnswer(returnsFirstArg());
    when(emis.save(any())).thenAnswer(returnsFirstArg());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  private InstallmentService.CreateCommand cmd(long total, int n, String sourceRef) {
    return new InstallmentService.CreateCommand(
        "RECEIVABLE", "cust1", "Rahul", "INVOICE", sourceRef, total, n, DUE, "MONTHLY");
  }

  private Installment installment(long total) {
    return new Installment(
        "inst1",
        "tenant1",
        InstallmentType.RECEIVABLE,
        "cust1",
        "Rahul",
        "INVOICE",
        "src1",
        total,
        3,
        Frequency.MONTHLY,
        clock.instant());
  }

  @Test
  void createScheduleGeneratesEmisAndEmits() {
    when(installments.existsByTenantIdAndSourceRef("tenant1", "src1")).thenReturn(false);

    Installment inst = service.createSchedule(cmd(120000, 3, "src1"));

    assertThat(inst.getOutstandingMinor()).isEqualTo(120000);
    verify(emis, times(3)).save(any(InstallmentEmi.class));
    verify(outbox).append(any(EventEnvelope.class));
    verify(audit).record(any(), any(), any(), any());
  }

  @Test
  void duplicateSourceIsRejected() {
    when(installments.existsByTenantIdAndSourceRef("tenant1", "src1")).thenReturn(true);
    assertThatThrownBy(() -> service.createSchedule(cmd(120000, 3, "src1")))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void payEmiReducesOutstandingAndEmits() {
    Installment inst = installment(120000);
    when(installments.findByTenantIdAndId("tenant1", "inst1")).thenReturn(Optional.of(inst));
    when(emis.findByInstallmentIdOrderByEmiNumber("inst1"))
        .thenReturn(
            List.of(
                new InstallmentEmi("e1", "tenant1", "inst1", 1, DUE, 40000),
                new InstallmentEmi("e2", "tenant1", "inst1", 2, DUE.plusMonths(1), 40000),
                new InstallmentEmi("e3", "tenant1", "inst1", 3, DUE.plusMonths(2), 40000)));

    Installment result = service.payEmi("inst1", 1, 40000);

    assertThat(result.getOutstandingMinor()).isEqualTo(80000);
    assertThat(result.isActive()).isTrue();
    verify(outbox).append(any(EventEnvelope.class));
  }

  @Test
  void payEmiClosesScheduleWhenFullyPaid() {
    Installment inst = installment(40000);
    when(installments.findByTenantIdAndId("tenant1", "inst1")).thenReturn(Optional.of(inst));
    when(emis.findByInstallmentIdOrderByEmiNumber("inst1"))
        .thenReturn(List.of(new InstallmentEmi("e1", "tenant1", "inst1", 1, DUE, 40000)));

    Installment result = service.payEmi("inst1", 1, 40000);

    assertThat(result.getOutstandingMinor()).isZero();
    assertThat(result.getStatus()).isEqualTo("CLOSED");
  }

  @Test
  void payEmiRejectsAlreadyPaid() {
    Installment inst = installment(40000);
    InstallmentEmi paid = new InstallmentEmi("e1", "tenant1", "inst1", 1, DUE, 40000);
    paid.apply(40000, clock.instant()); // fully paid already
    when(installments.findByTenantIdAndId("tenant1", "inst1")).thenReturn(Optional.of(inst));
    when(emis.findByInstallmentIdOrderByEmiNumber("inst1")).thenReturn(List.of(paid));

    assertThatThrownBy(() -> service.payEmi("inst1", 1, 10000))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("already fully paid");
  }

  @Test
  void modifyRejectsWhenAnyEmiPartiallyPaid() {
    Installment inst = installment(120000);
    InstallmentEmi partial = new InstallmentEmi("e1", "tenant1", "inst1", 1, DUE, 40000);
    partial.apply(10000, clock.instant()); // partial
    when(installments.findByTenantIdAndId("tenant1", "inst1")).thenReturn(Optional.of(inst));
    when(emis.findByInstallmentIdOrderByEmiNumber("inst1")).thenReturn(List.of(partial));

    assertThatThrownBy(() -> service.modifySchedule("inst1", 2, DUE.plusMonths(1), "MONTHLY"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("partially-paid");
  }
}
