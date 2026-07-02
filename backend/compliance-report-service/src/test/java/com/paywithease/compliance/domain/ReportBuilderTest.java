package com.paywithease.compliance.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ReportBuilderTest {

  private ReportBuilder.SourceView sales(long taxable, long tax, String supply) {
    return new ReportBuilder.SourceView("SALES", taxable, tax, 0, 0, supply);
  }

  private ReportBuilder.SourceView purchase(long taxable, long inputGst) {
    return new ReportBuilder.SourceView("PURCHASE", taxable, inputGst, 0, 0, null);
  }

  private ReportBuilder.SourceView payroll(long statutory, long tds) {
    return new ReportBuilder.SourceView("PAYROLL", 0, 0, statutory, tds, null);
  }

  @Test
  void gstr3bComputesNetPayable() {
    var built =
        ReportBuilder.build(
            ReportType.GSTR3B_SUMMARY,
            List.of(sales(1_000_000, 180_000, "B2B"), purchase(500_000, 90_000)));
    assertThat(built.totalTaxMinor()).isEqualTo(180_000); // output GST
    assertThat(built.netPayableMinor()).isEqualTo(90_000); // 180000 - 90000 ITC
    assertThat(built.lines()).hasSize(3);
    assertThat(built.missingFields()).isEmpty();
  }

  @Test
  void gstr3bFlagsItcExceedingOutput() {
    var built =
        ReportBuilder.build(
            ReportType.GSTR3B_SUMMARY,
            List.of(sales(100_000, 18_000, "B2B"), purchase(500_000, 90_000)));
    assertThat(built.netPayableMinor()).isZero(); // clamped at 0
    assertThat(built.missingFields()).anyMatch(m -> m.contains("ITC exceeds"));
  }

  @Test
  void salesRegisterSplitsB2bB2c() {
    var built =
        ReportBuilder.build(
            ReportType.SALES_REGISTER,
            List.of(sales(100_000, 18_000, "B2B"), sales(50_000, 9_000, "B2C")));
    assertThat(built.totalTaxableMinor()).isEqualTo(150_000);
    assertThat(built.totalTaxMinor()).isEqualTo(27_000);
  }

  @Test
  void emptyPeriodReportsMissingData() {
    var built = ReportBuilder.build(ReportType.GSTR3B_SUMMARY, List.of());
    assertThat(built.missingFields()).anyMatch(m -> m.contains("No GST activity"));
  }

  @Test
  void payrollComplianceSumsStatutoryAndTds() {
    var built =
        ReportBuilder.build(ReportType.PAYROLL_COMPLIANCE, List.of(payroll(155_000, 200_000)));
    assertThat(built.totalTaxMinor()).isEqualTo(355_000);
    assertThat(built.lines())
        .extracting(ReportBuilder.Line::label)
        .contains("PF / ESI / PT withheld", "Salary TDS");
  }

  @Test
  void tdsSummaryAlwaysFlagsVendorTdsGap() {
    var built = ReportBuilder.build(ReportType.TDS_SUMMARY, List.of(payroll(0, 200_000)));
    assertThat(built.totalTaxMinor()).isEqualTo(200_000);
    assertThat(built.missingFields()).anyMatch(m -> m.contains("Vendor/contractor TDS"));
  }
}
