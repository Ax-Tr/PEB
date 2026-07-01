package com.paywithease.employee.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.employee.domain.Employee;
import com.paywithease.employee.domain.SalaryStructure;
import com.paywithease.employee.infrastructure.EmployeeRepository;
import com.paywithease.employee.infrastructure.SalaryStructureRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Employee master and salary structure management. Scoped to the current tenant. Salary runs,
 * payslips, and statutory calculations are a later sprint and are not implemented here.
 */
@Service
public class EmployeeService {

  private final EmployeeRepository employees;
  private final SalaryStructureRepository salaryStructures;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public EmployeeService(
      EmployeeRepository employees,
      SalaryStructureRepository salaryStructures,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock) {
    this.employees = employees;
    this.salaryStructures = salaryStructures;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Transactional
  public Employee create(
      String name,
      String mobile,
      String email,
      String pan,
      String designation,
      LocalDate dateOfJoining) {
    String tenantId = TenantContext.requireTenantId();
    Instant now = clock.instant();
    Employee employee = new Employee(Ulid.newId(), tenantId, name, now);
    employee.setMobile(mobile);
    employee.setEmail(email);
    employee.setPan(pan);
    employee.setDesignation(designation);
    employee.setDateOfJoining(dateOfJoining);
    employees.save(employee);

    audit.record("EMPLOYEE_CREATED", "employee", employee.getId(), Map.of("name", name));
    emit("EMPLOYEE_CREATED", tenantId, employee.getId(), Map.of("name", name));
    return employee;
  }

  @Transactional(readOnly = true)
  public Employee get(String id) {
    Employee employee = employees.findById(id).orElseThrow(() -> ApiException.notFound("Employee"));
    requireSameTenant(employee);
    return employee;
  }

  @Transactional(readOnly = true)
  public List<Employee> list() {
    return employees.findByTenantIdOrderByCreatedAtDesc(TenantContext.requireTenantId());
  }

  @Transactional
  public SalaryStructure setSalaryStructure(
      String employeeId,
      long grossSalaryMinor,
      long basicMinor,
      long hraMinor,
      boolean pfApplicable,
      boolean esiApplicable,
      boolean ptApplicable,
      LocalDate effectiveFrom) {
    Employee employee = get(employeeId); // ensures employee exists + tenant match
    if (grossSalaryMinor <= 0) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "grossSalaryMinor must be positive");
    }
    if (basicMinor + hraMinor > grossSalaryMinor) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "components exceed gross");
    }

    SalaryStructure structure =
        salaryStructures
            .findByEmployeeId(employeeId)
            .orElseGet(() -> new SalaryStructure(Ulid.newId(), employee.getTenantId(), employeeId));
    structure.update(
        grossSalaryMinor,
        basicMinor,
        hraMinor,
        pfApplicable,
        esiApplicable,
        ptApplicable,
        effectiveFrom);
    salaryStructures.save(structure);

    audit.record(
        "SALARY_STRUCTURE_UPDATED",
        "employee",
        employeeId,
        Map.of("grossSalaryMinor", grossSalaryMinor));
    return structure;
  }

  @Transactional(readOnly = true)
  public SalaryStructure getSalaryStructure(String employeeId) {
    get(employeeId); // ensures employee exists + tenant match
    return salaryStructures
        .findByEmployeeId(employeeId)
        .orElseThrow(() -> ApiException.notFound("Salary structure"));
  }

  private void requireSameTenant(Employee employee) {
    if (!employee.getTenantId().equals(TenantContext.requireTenantId())) {
      throw ApiException.notFound("Employee");
    }
  }

  private void emit(String eventType, String tenantId, String aggregateId, Map<String, ?> data) {
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(tenantId)
            .businessId(tenantId)
            .sourceService("employee-payroll-service")
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(aggregateId)
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(objectMapper.valueToTree(data))
            .build(clock.instant());
    outbox.append(envelope);
  }
}
