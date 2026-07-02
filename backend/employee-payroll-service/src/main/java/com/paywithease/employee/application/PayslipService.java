package com.paywithease.employee.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.employee.domain.Employee;
import com.paywithease.employee.domain.SalaryRun;
import com.paywithease.employee.domain.SalaryRunLine;
import com.paywithease.employee.infrastructure.EmployeeRepository;
import com.paywithease.employee.infrastructure.SalaryRunLineRepository;
import java.time.Clock;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payslip document generation: renders a single payslip PDF on demand (read-only) and generates &
 * persists payslip document ids for every line in a run, emitting a PAYSLIP_GENERATED event per
 * line.
 */
@Service
public class PayslipService {

  private static final String SOURCE = "employee-payroll-service";

  private final SalaryRunService salaryRunService;
  private final EmployeeRepository employees;
  private final SalaryRunLineRepository lineRepo;
  private final PayslipPdfGenerator pdfGenerator;
  private final OutboxWriter outbox;
  private final AuditWriter audit;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public PayslipService(
      SalaryRunService salaryRunService,
      EmployeeRepository employees,
      SalaryRunLineRepository lineRepo,
      PayslipPdfGenerator pdfGenerator,
      OutboxWriter outbox,
      AuditWriter audit,
      ObjectMapper objectMapper,
      Clock clock) {
    this.salaryRunService = salaryRunService;
    this.employees = employees;
    this.lineRepo = lineRepo;
    this.pdfGenerator = pdfGenerator;
    this.outbox = outbox;
    this.audit = audit;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public byte[] renderPayslip(String runId, String lineId) {
    SalaryRun run = salaryRunService.get(runId); // tenant guard
    SalaryRunLine line =
        salaryRunService.lines(runId).stream()
            .filter(l -> l.getId().equals(lineId))
            .findFirst()
            .orElseThrow(() -> ApiException.notFound("Payslip line"));
    String employeeName =
        employees.findById(line.getEmployeeId()).map(Employee::getName).orElse("Employee");
    return pdfGenerator.generate(run, line, employeeName);
  }

  @Transactional
  public int generatePayslips(String runId) {
    salaryRunService.get(runId); // tenant guard
    int count = 0;
    for (SalaryRunLine line : salaryRunService.lines(runId)) {
      if (line.getPayslipDocumentId() != null) {
        continue;
      }
      String docId = Ulid.newId();
      line.attachPayslip(docId);
      lineRepo.save(line);
      audit.record(
          "PAYSLIP_GENERATED",
          "salary_run_line",
          line.getId(),
          Map.of("employeeId", line.getEmployeeId()));
      emit(runId, line, docId);
      count++;
    }
    return count;
  }

  private void emit(String runId, SalaryRunLine line, String docId) {
    String tenantId = TenantContext.requireTenantId();
    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("salaryRunId", runId);
    payload.put("lineId", line.getId());
    payload.put("employeeId", line.getEmployeeId());
    payload.put("netPayMinor", line.getNetPayMinor());
    payload.put("documentId", docId);
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType("PAYSLIP_GENERATED")
            .tenantId(tenantId)
            .businessId(tenantId)
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(line.getId())
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }
}
