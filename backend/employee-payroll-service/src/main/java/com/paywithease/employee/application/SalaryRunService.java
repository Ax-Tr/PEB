package com.paywithease.employee.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.employee.domain.Employee;
import com.paywithease.employee.domain.SalaryRun;
import com.paywithease.employee.domain.SalaryRunLine;
import com.paywithease.employee.domain.SalaryStructure;
import com.paywithease.employee.domain.payroll.PayrollCalculator;
import com.paywithease.employee.domain.payroll.PayrollRates;
import com.paywithease.employee.infrastructure.EmployeeRepository;
import com.paywithease.employee.infrastructure.SalaryRunLineRepository;
import com.paywithease.employee.infrastructure.SalaryRunRepository;
import com.paywithease.employee.infrastructure.SalaryStructureRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs monthly payroll: for each active employee with a salary structure, computes the payslip via
 * {@link PayrollCalculator} and persists a line. A run is idempotent per (tenant, year, month) — a
 * month cannot be double-run. Emits SALARY_RUN_CREATED (the ledger posts the payroll journal).
 */
@Service
public class SalaryRunService {

  private static final String SOURCE = "employee-payroll-service";

  private final SalaryRunRepository runs;
  private final SalaryRunLineRepository lines;
  private final EmployeeRepository employees;
  private final SalaryStructureRepository structures;
  private final PayrollRates rates;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public SalaryRunService(
      SalaryRunRepository runs,
      SalaryRunLineRepository lines,
      EmployeeRepository employees,
      SalaryStructureRepository structures,
      PayrollRates rates,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock) {
    this.runs = runs;
    this.lines = lines;
    this.employees = employees;
    this.structures = structures;
    this.rates = rates;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  /** Per-employee variable inputs for a run (defaults to zero when omitted). */
  public record Adjustment(
      int lopDays, long incentivesMinor, long otherDeductionsMinor, long tdsMinor) {
    public static Adjustment none() {
      return new Adjustment(0, 0, 0, 0);
    }
  }

  @Transactional
  public SalaryRun process(
      int year, int month, int workingDays, Map<String, Adjustment> adjustments, String createdBy) {
    String tenantId = TenantContext.requireTenantId();
    if (month < 1 || month > 12) {
      throw new ApiException(ErrorCode.VALIDATION_FAILED, "month must be 1..12");
    }
    if (runs.existsByTenantIdAndYearAndMonth(tenantId, year, month)) {
      throw new ApiException(
          ErrorCode.CONFLICT, "Salary run already processed for " + year + "-" + month);
    }

    Instant now = clock.instant();
    SalaryRun run = new SalaryRun(Ulid.newId(), tenantId, year, month, workingDays, createdBy, now);
    runs.save(run);

    for (Employee employee : employees.findByTenantIdOrderByCreatedAtDesc(tenantId)) {
      if (!"ACTIVE".equals(employee.getStatus())) {
        continue;
      }
      SalaryStructure structure = structures.findByEmployeeId(employee.getId()).orElse(null);
      if (structure == null) {
        continue; // no salary structure → not payable this run
      }
      Adjustment adj = adjustments.getOrDefault(employee.getId(), Adjustment.none());
      PayrollCalculator.Result result =
          PayrollCalculator.compute(
              new PayrollCalculator.Input(
                  structure.getGrossSalaryMinor(),
                  structure.getBasicMinor(),
                  workingDays,
                  adj.lopDays(),
                  adj.incentivesMinor(),
                  adj.otherDeductionsMinor(),
                  adj.tdsMinor(),
                  structure.isPfApplicable(),
                  structure.isEsiApplicable(),
                  structure.isPtApplicable()),
              rates);

      lines.save(
          new SalaryRunLine(
              Ulid.newId(),
              tenantId,
              run.getId(),
              employee.getId(),
              structure.getGrossSalaryMinor(),
              structure.getBasicMinor(),
              adj.lopDays(),
              result));
      run.addTotals(
          result.totalEarningsMinor(),
          result.netPayMinor(),
          result.statutoryWithheldMinor(),
          result.tdsMinor());
    }
    runs.save(run);

    audit.record(
        "SALARY_RUN_CREATED",
        "salary_run",
        run.getId(),
        Map.of("year", year, "month", month, "employeeCount", run.getEmployeeCount()));
    emit(run);
    return run;
  }

  @Transactional(readOnly = true)
  public SalaryRun get(String id) {
    return runs.findByTenantIdAndId(TenantContext.requireTenantId(), id)
        .orElseThrow(() -> ApiException.notFound("Salary run"));
  }

  @Transactional(readOnly = true)
  public List<SalaryRunLine> lines(String runId) {
    get(runId); // tenant guard
    return lines.findBySalaryRunId(runId);
  }

  private void emit(SalaryRun run) {
    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("salaryRunId", run.getId());
    payload.put("year", run.getYear());
    payload.put("month", run.getMonth());
    payload.put("totalEarningsMinor", run.getTotalEarningsMinor());
    payload.put("totalNetMinor", run.getTotalNetMinor());
    payload.put("totalStatutoryMinor", run.getTotalStatutoryMinor());
    payload.put("totalTdsMinor", run.getTotalTdsMinor());
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType("SALARY_RUN_CREATED")
            .tenantId(run.getTenantId())
            .businessId(run.getTenantId())
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(run.getId())
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }
}
