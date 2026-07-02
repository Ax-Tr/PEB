package com.paywithease.compliance.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.compliance.domain.ComplianceReport;
import com.paywithease.compliance.domain.ComplianceReportLine;
import com.paywithease.compliance.domain.ReportBuilder;
import com.paywithease.compliance.domain.ReportType;
import com.paywithease.compliance.domain.SourceRecord;
import com.paywithease.compliance.infrastructure.ComplianceReportLineRepository;
import com.paywithease.compliance.infrastructure.ComplianceReportRepository;
import com.paywithease.compliance.infrastructure.SourceRecordRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prepares GST/payroll/TDS/ITR compliance reports from a period read-model, governed by the report
 * status lifecycle. Reports are preparation artifacts — approval requires reconciled data, and a
 * report is only FILED with an official acknowledgement.
 */
@Service
public class ComplianceReportService {

  private static final String SOURCE = "compliance-report-service";

  private final SourceRecordRepository sources;
  private final ComplianceReportRepository reports;
  private final ComplianceReportLineRepository lines;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public ComplianceReportService(
      SourceRecordRepository sources,
      ComplianceReportRepository reports,
      ComplianceReportLineRepository lines,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock) {
    this.sources = sources;
    this.reports = reports;
    this.lines = lines;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  public record SourceCommand(
      String recordType,
      int year,
      int month,
      long taxableMinor,
      long taxMinor,
      long statutoryMinor,
      long tdsMinor,
      String supplyType,
      String sourceRef,
      String reference) {}

  /**
   * Records a period source row from an upstream event (idempotent per (recordType, sourceRef)).
   */
  @Transactional
  public void recordSource(SourceCommand cmd) {
    String tenantId = TenantContext.requireTenantId();
    if (sources.existsByTenantIdAndRecordTypeAndSourceRef(
        tenantId, cmd.recordType(), cmd.sourceRef())) {
      return; // idempotent
    }
    sources.save(
        new SourceRecord(
            Ulid.newId(),
            tenantId,
            cmd.recordType(),
            cmd.year(),
            cmd.month(),
            cmd.taxableMinor(),
            cmd.taxMinor(),
            cmd.statutoryMinor(),
            cmd.tdsMinor(),
            cmd.supplyType(),
            cmd.sourceRef(),
            cmd.reference(),
            clock.instant()));
  }

  @Transactional
  public ComplianceReport generate(ReportType type, int year, int month) {
    String tenantId = TenantContext.requireTenantId();
    List<ReportBuilder.SourceView> views =
        sources.findByTenantIdAndYearAndMonth(tenantId, year, month).stream()
            .map(
                s ->
                    new ReportBuilder.SourceView(
                        s.getRecordType(),
                        s.getTaxableMinor(),
                        s.getTaxMinor(),
                        s.getStatutoryMinor(),
                        s.getTdsMinor(),
                        s.getSupplyType()))
            .toList();
    ReportBuilder.Built built = ReportBuilder.build(type, views);

    ComplianceReport report =
        reports
            .findByTenantIdAndTypeAndYearAndMonth(tenantId, type.name(), year, month)
            .orElse(null);
    if (report != null) {
      if (!"DRAFT".equals(report.getStatus())) {
        throw new ApiException(
            ErrorCode.CONFLICT, "Report is " + report.getStatus() + " and cannot be regenerated");
      }
      lines.deleteByReportId(report.getId());
    } else {
      report = new ComplianceReport(Ulid.newId(), tenantId, type, year, month, clock.instant());
    }
    report.setTotals(
        built.totalTaxableMinor(),
        built.totalTaxMinor(),
        built.netPayableMinor(),
        writeJson(built.missingFields()));
    reports.save(report);

    for (ReportBuilder.Line l : built.lines()) {
      lines.save(
          new ComplianceReportLine(
              Ulid.newId(),
              tenantId,
              report.getId(),
              l.label(),
              l.taxableMinor(),
              l.taxMinor(),
              l.amountMinor()));
    }
    audit.record(
        "COMPLIANCE_REPORT_GENERATED",
        "compliance_report",
        report.getId(),
        Map.of("type", type.name(), "year", year, "month", month));
    emit("COMPLIANCE_REPORT_GENERATED", report);
    return report;
  }

  @Transactional
  public ComplianceReport setReconciled(String reportId, boolean reconciled) {
    ComplianceReport report = load(reportId);
    report.setReconciled(reconciled);
    reports.save(report);
    audit.record(
        "COMPLIANCE_REPORT_RECONCILED_FLAG",
        "compliance_report",
        reportId,
        Map.of("reconciled", reconciled));
    return report;
  }

  @Transactional
  public ComplianceReport review(String reportId, String actorId) {
    ComplianceReport report = load(reportId);
    report.markReviewed(actorId);
    reports.save(report);
    audit.record("COMPLIANCE_REPORT_REVIEWED", "compliance_report", reportId, Map.of());
    return report;
  }

  @Transactional
  public ComplianceReport approve(String reportId, String actorId) {
    ComplianceReport report = load(reportId);
    report.approve(actorId);
    reports.save(report);
    audit.record("COMPLIANCE_REPORT_APPROVED", "compliance_report", reportId, Map.of());
    return report;
  }

  @Transactional
  public ComplianceReport recordFiling(String reportId, String ackReference, String actorId) {
    ComplianceReport report = load(reportId);
    report.recordFiling(ackReference, clock.instant());
    reports.save(report);
    audit.record(
        "COMPLIANCE_REPORT_FILED",
        "compliance_report",
        reportId,
        Map.of("ack", ackReference, "by", actorId));
    return report;
  }

  @Transactional(readOnly = true)
  public ComplianceReport get(String reportId) {
    return load(reportId);
  }

  @Transactional(readOnly = true)
  public List<ComplianceReportLine> lines(String reportId) {
    load(reportId);
    return lines.findByReportId(reportId);
  }

  @Transactional(readOnly = true)
  public List<ComplianceReport> list() {
    return reports.findByTenantIdOrderByGeneratedAtDesc(TenantContext.requireTenantId());
  }

  private ComplianceReport load(String reportId) {
    return reports
        .findByTenantIdAndId(TenantContext.requireTenantId(), reportId)
        .orElseThrow(() -> ApiException.notFound("Compliance report"));
  }

  private void emit(String eventType, ComplianceReport report) {
    var payload =
        objectMapper
            .createObjectNode()
            .put("reportId", report.getId())
            .put("type", report.getType())
            .put("year", report.getYear())
            .put("month", report.getMonth())
            .put("status", report.getStatus());
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(report.getTenantId())
            .businessId(report.getTenantId())
            .sourceService(SOURCE)
            .actorId(TenantContext.actorId().orElse(null))
            .aggregateId(report.getId())
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }

  private String writeJson(List<String> missing) {
    try {
      return objectMapper.writeValueAsString(missing == null ? List.of() : missing);
    } catch (Exception e) {
      return "[]";
    }
  }
}
