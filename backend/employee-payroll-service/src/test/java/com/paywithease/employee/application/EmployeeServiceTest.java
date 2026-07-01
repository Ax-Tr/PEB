package com.paywithease.employee.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.employee.domain.Employee;
import com.paywithease.employee.domain.SalaryStructure;
import com.paywithease.employee.infrastructure.EmployeeRepository;
import com.paywithease.employee.infrastructure.SalaryStructureRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

  @Mock EmployeeRepository employees;
  @Mock SalaryStructureRepository salaryStructures;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private EmployeeService service;
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service =
        new EmployeeService(employees, salaryStructures, audit, outbox, new ObjectMapper(), clock);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void createEmitsEvent() {
    when(employees.save(any(Employee.class))).thenAnswer(returnsFirstArg());

    Employee e =
        service.create(
            "Asha Rao", "+919812345678", "asha@example.com", "AAPFU0939F", "Analyst", null);

    assertThat(e.getTenantId()).isEqualTo("tenant1");
    assertThat(e.getName()).isEqualTo("Asha Rao");
    verify(employees).save(any(Employee.class));
    verify(outbox).append(any(EventEnvelope.class));
    verify(audit).record(any(), any(), any(), any());
  }

  @Test
  void setSalaryStructurePersists() {
    Employee employee = new Employee("emp1", "tenant1", "Asha Rao", clock.instant());
    when(employees.findById("emp1")).thenReturn(Optional.of(employee));
    when(salaryStructures.findByEmployeeId("emp1")).thenReturn(Optional.empty());
    when(salaryStructures.save(any(SalaryStructure.class))).thenAnswer(returnsFirstArg());

    SalaryStructure s =
        service.setSalaryStructure(
            "emp1",
            5_000_000L,
            2_500_000L,
            1_000_000L,
            true,
            false,
            true,
            LocalDate.of(2026, 7, 1));

    assertThat(s.getGrossSalaryMinor()).isEqualTo(5_000_000L);
    assertThat(s.getBasicMinor()).isEqualTo(2_500_000L);
    assertThat(s.isPfApplicable()).isTrue();
    verify(salaryStructures).save(any(SalaryStructure.class));
    verify(audit).record(any(), any(), any(), any());
  }

  @Test
  void setSalaryStructureRejectsComponentsOverGross() {
    Employee employee = new Employee("emp1", "tenant1", "Asha Rao", clock.instant());
    when(employees.findById("emp1")).thenReturn(Optional.of(employee));

    assertThatThrownBy(
            () ->
                service.setSalaryStructure(
                    "emp1", 5_000_000L, 4_000_000L, 2_000_000L, false, false, false, null))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("components exceed gross");
  }
}
