package com.paywithease.employee.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.employee.domain.Employee;
import com.paywithease.employee.domain.SalaryRun;
import com.paywithease.employee.domain.SalaryRunLine;
import com.paywithease.employee.domain.payroll.PayrollCalculator;
import com.paywithease.employee.domain.payroll.PayrollRates;
import com.paywithease.employee.infrastructure.EmployeeRepository;
import com.paywithease.employee.infrastructure.SalaryRunLineRepository;
import java.time.Clock;
import java.time.Instant;
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
class PayslipServiceTest {

  @Mock SalaryRunService salaryRunService;
  @Mock EmployeeRepository employees;
  @Mock SalaryRunLineRepository lineRepo;
  @Mock PayslipPdfGenerator pdfGenerator;
  @Mock OutboxWriter outbox;
  @Mock AuditWriter audit;

  private PayslipService service;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-05-31T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service =
        new PayslipService(
            salaryRunService,
            employees,
            lineRepo,
            pdfGenerator,
            outbox,
            audit,
            objectMapper,
            clock);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
    when(lineRepo.save(any())).thenAnswer(returnsFirstArg());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  private SalaryRun run() {
    return new SalaryRun(
        "run1", "tenant1", 2026, 5, 30, "actor1", Instant.parse("2026-05-31T00:00:00Z"));
  }

  private SalaryRunLine line() {
    PayrollCalculator.Result result =
        PayrollCalculator.compute(
            new PayrollCalculator.Input(2_000_000L, 1_000_000L, 30, 0, 0, 0, 0, true, true, true),
            PayrollRates.defaults());
    return new SalaryRunLine("line1", "tenant1", "run1", "emp1", 2_000_000L, 1_000_000L, 0, result);
  }

  @Test
  void generatePayslipsAttachesDocumentAndEmitsEvent() {
    SalaryRunLine line = line();
    when(salaryRunService.get("run1")).thenReturn(run());
    when(salaryRunService.lines("run1")).thenReturn(List.of(line));

    int generated = service.generatePayslips("run1");

    assertThat(generated).isEqualTo(1);
    assertThat(line.getPayslipDocumentId()).isNotNull();
    verify(lineRepo, times(1)).save(line);
    verify(outbox, times(1)).append(any(EventEnvelope.class));
  }

  @Test
  void renderPayslipReturnsPdfBytes() {
    SalaryRunLine line = line();
    when(salaryRunService.get("run1")).thenReturn(run());
    when(salaryRunService.lines("run1")).thenReturn(List.of(line));
    when(employees.findById("emp1"))
        .thenReturn(Optional.of(new Employee("emp1", "tenant1", "Ravi", clock.instant())));
    byte[] pdf = new byte[] {37, 80, 68, 70};
    when(pdfGenerator.generate(any(), any(), any())).thenReturn(pdf);

    byte[] bytes = service.renderPayslip("run1", "line1");

    assertThat(bytes).isEqualTo(pdf);
  }
}
