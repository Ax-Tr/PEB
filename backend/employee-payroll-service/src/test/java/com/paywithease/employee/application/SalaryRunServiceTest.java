package com.paywithease.employee.application;

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
import com.paywithease.employee.domain.Employee;
import com.paywithease.employee.domain.SalaryRun;
import com.paywithease.employee.domain.SalaryRunLine;
import com.paywithease.employee.domain.SalaryStructure;
import com.paywithease.employee.domain.payroll.PayrollRates;
import com.paywithease.employee.infrastructure.EmployeeRepository;
import com.paywithease.employee.infrastructure.SalaryRunLineRepository;
import com.paywithease.employee.infrastructure.SalaryRunRepository;
import com.paywithease.employee.infrastructure.SalaryStructureRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
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
class SalaryRunServiceTest {

  @Mock SalaryRunRepository runs;
  @Mock SalaryRunLineRepository lines;
  @Mock EmployeeRepository employees;
  @Mock SalaryStructureRepository structures;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private SalaryRunService service;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-05-31T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service =
        new SalaryRunService(
            runs,
            lines,
            employees,
            structures,
            PayrollRates.defaults(),
            audit,
            outbox,
            objectMapper,
            clock);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
    when(runs.save(any())).thenAnswer(returnsFirstArg());
    when(lines.save(any())).thenAnswer(returnsFirstArg());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  private Employee employee(String id) {
    return new Employee(id, "tenant1", "Ravi", clock.instant());
  }

  private SalaryStructure structure(String employeeId, long gross, long basic) {
    SalaryStructure s = new SalaryStructure("ss-" + employeeId, "tenant1", employeeId);
    s.update(gross, basic, 0, true, true, true, LocalDate.of(2026, 4, 1));
    return s;
  }

  @Test
  void processComputesLinesAndEmitsEvent() {
    when(runs.existsByTenantIdAndYearAndMonth("tenant1", 2026, 5)).thenReturn(false);
    when(employees.findByTenantIdOrderByCreatedAtDesc("tenant1"))
        .thenReturn(List.of(employee("emp1")));
    when(structures.findByEmployeeId("emp1"))
        .thenReturn(Optional.of(structure("emp1", 2_000_000, 1_000_000)));

    SalaryRun run = service.process(2026, 5, 30, Map.of(), "actor1");

    assertThat(run.getEmployeeCount()).isEqualTo(1);
    // Net for ₹20,000 gross w/ PF 1,200 + ESI 150 + PT 200 = ₹18,450
    assertThat(run.getTotalNetMinor()).isEqualTo(1_845_000);
    assertThat(run.getTotalStatutoryMinor()).isEqualTo(155_000);
    verify(lines, times(1)).save(any(SalaryRunLine.class));

    ArgumentCaptor<EventEnvelope> captor = ArgumentCaptor.forClass(EventEnvelope.class);
    verify(outbox).append(captor.capture());
    assertThat(captor.getValue().eventType()).isEqualTo("SALARY_RUN_CREATED");
    verify(audit).record(any(), any(), any(), any());
  }

  @Test
  void cannotRunSameMonthTwice() {
    when(runs.existsByTenantIdAndYearAndMonth("tenant1", 2026, 5)).thenReturn(true);
    assertThatThrownBy(() -> service.process(2026, 5, 30, Map.of(), "actor1"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("already processed");
  }

  @Test
  void skipsEmployeesWithoutSalaryStructure() {
    when(runs.existsByTenantIdAndYearAndMonth("tenant1", 2026, 5)).thenReturn(false);
    when(employees.findByTenantIdOrderByCreatedAtDesc("tenant1"))
        .thenReturn(List.of(employee("emp1")));
    when(structures.findByEmployeeId("emp1")).thenReturn(Optional.empty());

    SalaryRun run = service.process(2026, 5, 30, Map.of(), "actor1");

    assertThat(run.getEmployeeCount()).isZero();
    verify(lines, org.mockito.Mockito.never()).save(any());
  }
}
