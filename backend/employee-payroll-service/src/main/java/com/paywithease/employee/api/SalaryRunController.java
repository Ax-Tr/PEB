package com.paywithease.employee.api;

import com.paywithease.employee.application.PayslipService;
import com.paywithease.employee.application.SalaryRunService;
import com.paywithease.employee.domain.SalaryRun;
import com.paywithease.employee.domain.SalaryRunLine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Monthly salary-run and payslip API. */
@RestController
@RequestMapping("/api/v1/salary-runs")
@Tag(name = "salary-runs", description = "Monthly payroll runs and payslip generation")
public class SalaryRunController {

  private final SalaryRunService salaryRunService;
  private final PayslipService payslipService;

  public SalaryRunController(SalaryRunService salaryRunService, PayslipService payslipService) {
    this.salaryRunService = salaryRunService;
    this.payslipService = payslipService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Process a monthly salary run for all payable employees")
  public SalaryRunDtos.SalaryRunResponse process(
      @Valid @RequestBody SalaryRunDtos.ProcessRunRequest body, @AuthenticationPrincipal Jwt jwt) {
    Map<String, SalaryRunService.Adjustment> adjustments = toAdjustments(body.adjustments());
    SalaryRun run =
        salaryRunService.process(
            body.year(), body.month(), body.workingDays(), adjustments, jwt.getSubject());
    return toResponse(run);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get a salary run with its totals")
  public SalaryRunDtos.SalaryRunResponse get(@PathVariable String id) {
    return toResponse(salaryRunService.get(id));
  }

  @GetMapping("/{id}/lines")
  @Operation(summary = "List the per-employee payslip lines of a salary run")
  public List<SalaryRunDtos.SalaryRunLineResponse> lines(@PathVariable String id) {
    return salaryRunService.lines(id).stream().map(SalaryRunController::toLine).toList();
  }

  @GetMapping("/{id}/lines/{lineId}/payslip")
  @Operation(summary = "Render an employee's payslip as a PDF")
  public ResponseEntity<byte[]> payslip(@PathVariable String id, @PathVariable String lineId) {
    byte[] bytes = payslipService.renderPayslip(id, lineId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"payslip-" + lineId + ".pdf\"")
        .body(bytes);
  }

  @PostMapping("/{id}/generate-payslips")
  @Operation(summary = "Generate payslip documents for every line in a run")
  public SalaryRunDtos.GeneratePayslipsResponse generatePayslips(@PathVariable String id) {
    return new SalaryRunDtos.GeneratePayslipsResponse(payslipService.generatePayslips(id));
  }

  private static Map<String, SalaryRunService.Adjustment> toAdjustments(
      Map<String, SalaryRunDtos.AdjustmentDto> adjustments) {
    if (adjustments == null) {
      return Map.of();
    }
    return adjustments.entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                e ->
                    new SalaryRunService.Adjustment(
                        e.getValue().lopDays(),
                        e.getValue().incentivesMinor(),
                        e.getValue().otherDeductionsMinor(),
                        e.getValue().tdsMinor())));
  }

  private static SalaryRunDtos.SalaryRunResponse toResponse(SalaryRun run) {
    return new SalaryRunDtos.SalaryRunResponse(
        run.getId(),
        run.getYear(),
        run.getMonth(),
        run.getWorkingDays(),
        run.getStatus(),
        run.getTotalEarningsMinor(),
        run.getTotalNetMinor(),
        run.getTotalStatutoryMinor(),
        run.getTotalTdsMinor(),
        run.getEmployeeCount());
  }

  private static SalaryRunDtos.SalaryRunLineResponse toLine(SalaryRunLine line) {
    return new SalaryRunDtos.SalaryRunLineResponse(
        line.getId(),
        line.getEmployeeId(),
        line.getGrossMinor(),
        line.getEarnedGrossMinor(),
        line.getIncentivesMinor(),
        line.getPfMinor(),
        line.getEsiMinor(),
        line.getPtMinor(),
        line.getTdsMinor(),
        line.getOtherDeductionsMinor(),
        line.getNetPayMinor(),
        line.getPayslipDocumentId());
  }
}
