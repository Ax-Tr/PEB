package com.paywithease.compliance.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.compliance.application.ComplianceReportService;
import com.paywithease.compliance.domain.ReportType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Compliance report API — generate GST/payroll/TDS/ITR reports and drive the DRAFT → REVIEWED →
 * APPROVED → FILED lifecycle.
 *
 * <p>Maker-checker is enforced with DISTINCT authorities: a report is REVIEWED by an accountant/CA
 * and APPROVED by an owner (a separate, higher-privilege actor). Approval additionally requires the
 * underlying data to be reconciled (enforced in the domain). A report only reaches FILED when an
 * external portal/API acknowledgement is recorded — recording it here does NOT itself file with the
 * portal.
 */
@RestController
@RequestMapping("/api/v1/compliance")
@Tag(
    name = "compliance",
    description = "Prepares GST/payroll/TDS/ITR compliance reports with a maker-checker lifecycle")
public class ComplianceReportController {

  private final ComplianceReportService service;
  private final ObjectMapper objectMapper;

  public ComplianceReportController(ComplianceReportService service, ObjectMapper objectMapper) {
    this.service = service;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/reports/generate")
  @PreAuthorize("hasAnyRole('ACCOUNTANT','OWNER')")
  @Operation(summary = "Generate (or regenerate a DRAFT) compliance report for a period")
  public ComplianceDtos.ReportResponse generate(
      @Valid @RequestBody ComplianceDtos.GenerateReportRequest body) {
    ReportType type = ComplianceDtos.parseType(body.type());
    return ComplianceDtos.toReport(service.generate(type, body.year(), body.month()), objectMapper);
  }

  @GetMapping("/reports")
  @Operation(summary = "List compliance reports for the tenant (newest first)")
  public List<ComplianceDtos.ReportResponse> list() {
    return service.list().stream().map(r -> ComplianceDtos.toReport(r, objectMapper)).toList();
  }

  @GetMapping("/reports/{id}")
  @Operation(summary = "Get a compliance report")
  public ComplianceDtos.ReportResponse get(@PathVariable String id) {
    return ComplianceDtos.toReport(service.get(id), objectMapper);
  }

  @GetMapping("/reports/{id}/lines")
  @Operation(summary = "List the lines of a compliance report")
  public List<ComplianceDtos.ReportLineResponse> lines(@PathVariable String id) {
    return service.lines(id).stream().map(ComplianceDtos::toLine).toList();
  }

  @PostMapping("/reports/{id}/reconciled")
  @PreAuthorize("hasAnyRole('ACCOUNTANT','OWNER')")
  @Operation(summary = "Set whether the report's underlying data is reconciled")
  public ComplianceDtos.ReportResponse setReconciled(
      @PathVariable String id, @Valid @RequestBody ComplianceDtos.ReconciledRequest body) {
    return ComplianceDtos.toReport(service.setReconciled(id, body.reconciled()), objectMapper);
  }

  @PostMapping("/reports/{id}/review")
  @PreAuthorize("hasAnyRole('CA','ACCOUNTANT')")
  @Operation(summary = "Maker-checker (reviewer): mark a DRAFT report REVIEWED")
  public ComplianceDtos.ReportResponse review(
      @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    return ComplianceDtos.toReport(service.review(id, jwt.getSubject()), objectMapper);
  }

  @PostMapping("/reports/{id}/approve")
  @PreAuthorize("hasAnyRole('OWNER','CO_OWNER')")
  @Operation(
      summary =
          "Maker-checker (approver): approve a REVIEWED report. High-risk step guarded by an"
              + " authority distinct from review; requires reconciled data.")
  public ComplianceDtos.ReportResponse approve(
      @PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
    return ComplianceDtos.toReport(service.approve(id, jwt.getSubject()), objectMapper);
  }

  @PostMapping("/reports/{id}/filing")
  @PreAuthorize("hasAnyRole('OWNER','ACCOUNTANT')")
  @Operation(
      summary =
          "Record an EXTERNAL filing acknowledgement (marks the report FILED). This only records an"
              + " acknowledgement obtained out-of-band; it does NOT file with the tax portal.")
  public ComplianceDtos.ReportResponse filing(
      @PathVariable String id,
      @Valid @RequestBody ComplianceDtos.FilingRequest body,
      @AuthenticationPrincipal Jwt jwt) {
    return ComplianceDtos.toReport(
        service.recordFiling(id, body.ackReference(), jwt.getSubject()), objectMapper);
  }
}
