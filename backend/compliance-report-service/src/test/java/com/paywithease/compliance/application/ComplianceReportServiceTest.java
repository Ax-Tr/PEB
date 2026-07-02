package com.paywithease.compliance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.compliance.domain.ComplianceReport;
import com.paywithease.compliance.domain.ReportType;
import com.paywithease.compliance.domain.SourceRecord;
import com.paywithease.compliance.infrastructure.ComplianceReportLineRepository;
import com.paywithease.compliance.infrastructure.ComplianceReportRepository;
import com.paywithease.compliance.infrastructure.SourceRecordRepository;
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
class ComplianceReportServiceTest {

  @Mock SourceRecordRepository sources;
  @Mock ComplianceReportRepository reports;
  @Mock ComplianceReportLineRepository lines;
  @Mock AuditWriter audit;
  @Mock OutboxWriter outbox;

  private ComplianceReportService service;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service =
        new ComplianceReportService(sources, reports, lines, audit, outbox, objectMapper, clock);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "actor1", "corr1"));
    when(reports.save(any())).thenAnswer(returnsFirstArg());
    when(lines.save(any())).thenAnswer(returnsFirstArg());
    when(sources.save(any())).thenAnswer(returnsFirstArg());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  private SourceRecord sales(long taxable, long tax, String supply, String ref) {
    return new SourceRecord(
        "s-" + ref,
        "tenant1",
        "SALES",
        2026,
        5,
        taxable,
        tax,
        0,
        0,
        supply,
        ref,
        ref,
        clock.instant());
  }

  @Test
  void recordSourceIsIdempotent() {
    when(sources.existsByTenantIdAndRecordTypeAndSourceRef("tenant1", "SALES", "INV-1"))
        .thenReturn(true);
    service.recordSource(
        new ComplianceReportService.SourceCommand(
            "SALES", 2026, 5, 100_000, 18_000, 0, 0, "B2B", "INV-1", "Invoice INV-1"));
    verify(sources, never()).save(any());
  }

  @Test
  void recordSourcePersistsNewRow() {
    when(sources.existsByTenantIdAndRecordTypeAndSourceRef("tenant1", "SALES", "INV-2"))
        .thenReturn(false);
    service.recordSource(
        new ComplianceReportService.SourceCommand(
            "SALES", 2026, 5, 100_000, 18_000, 0, 0, "B2B", "INV-2", "Invoice INV-2"));
    verify(sources).save(any(SourceRecord.class));
  }

  @Test
  void generateBuildsReportAndPersistsLines() {
    when(sources.findByTenantIdAndYearAndMonth("tenant1", 2026, 5))
        .thenReturn(List.of(sales(100_000, 18_000, "B2B", "INV-1")));
    when(reports.findByTenantIdAndTypeAndYearAndMonth("tenant1", "GSTR3B_SUMMARY", 2026, 5))
        .thenReturn(Optional.empty());

    ComplianceReport report = service.generate(ReportType.GSTR3B_SUMMARY, 2026, 5);

    assertThat(report.getStatus()).isEqualTo("DRAFT");
    assertThat(report.getTotalTaxMinor()).isEqualTo(18_000);
    verify(lines, atLeastOnce()).save(any());
    verify(outbox).append(any());
    verify(audit).record(any(), any(), any(), any());
  }

  @Test
  void regenerateReplacesLinesWhenStillDraft() {
    ComplianceReport existing =
        new ComplianceReport("r1", "tenant1", ReportType.GSTR3B_SUMMARY, 2026, 5, clock.instant());
    when(sources.findByTenantIdAndYearAndMonth("tenant1", 2026, 5))
        .thenReturn(List.of(sales(100_000, 18_000, "B2B", "INV-1")));
    when(reports.findByTenantIdAndTypeAndYearAndMonth("tenant1", "GSTR3B_SUMMARY", 2026, 5))
        .thenReturn(Optional.of(existing));

    service.generate(ReportType.GSTR3B_SUMMARY, 2026, 5);

    verify(lines).deleteByReportId("r1");
    verify(lines, atLeastOnce()).save(any());
  }

  @Test
  void cannotRegenerateApprovedReport() {
    ComplianceReport approved =
        new ComplianceReport("r1", "tenant1", ReportType.GSTR3B_SUMMARY, 2026, 5, clock.instant());
    approved.markReviewed("ca1");
    approved.setReconciled(true);
    approved.approve("owner1");
    when(sources.findByTenantIdAndYearAndMonth("tenant1", 2026, 5)).thenReturn(List.of());
    when(reports.findByTenantIdAndTypeAndYearAndMonth("tenant1", "GSTR3B_SUMMARY", 2026, 5))
        .thenReturn(Optional.of(approved));

    assertThatThrownBy(() -> service.generate(ReportType.GSTR3B_SUMMARY, 2026, 5))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("cannot be regenerated");
  }

  @Test
  void approveRequiresReconciliationThenFilesWithAck() {
    ComplianceReport report =
        new ComplianceReport("r1", "tenant1", ReportType.GSTR3B_SUMMARY, 2026, 5, clock.instant());
    when(reports.findByTenantIdAndId("tenant1", "r1")).thenReturn(Optional.of(report));

    service.review("r1", "ca1");
    assertThatThrownBy(() -> service.approve("r1", "owner1"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("reconciled");

    service.setReconciled("r1", true);
    service.approve("r1", "owner1");
    assertThat(report.getStatus()).isEqualTo("APPROVED");

    ComplianceReport filed = service.recordFiling("r1", "ACK-123", "owner1");
    assertThat(filed.getStatus()).isEqualTo("FILED");
    assertThat(filed.getAckReference()).isEqualTo("ACK-123");
  }

  @Test
  void getMissingReportThrowsNotFound() {
    when(reports.findByTenantIdAndId("tenant1", "nope")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.get("nope")).isInstanceOf(ApiException.class);
  }
}
