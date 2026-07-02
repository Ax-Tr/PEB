package com.paywithease.compliance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A single line of a compliance report (e.g. an Output-GST or ITC row). */
@Entity
@Table(name = "compliance_report_lines")
public class ComplianceReportLine {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(name = "report_id", length = 26, nullable = false)
  private String reportId;

  @Column(nullable = false)
  private String label;

  @Column(name = "taxable_minor", nullable = false)
  private long taxableMinor;

  @Column(name = "tax_minor", nullable = false)
  private long taxMinor;

  @Column(name = "amount_minor", nullable = false)
  private long amountMinor;

  protected ComplianceReportLine() {}

  public ComplianceReportLine(
      String id,
      String tenantId,
      String reportId,
      String label,
      long taxableMinor,
      long taxMinor,
      long amountMinor) {
    this.id = id;
    this.tenantId = tenantId;
    this.reportId = reportId;
    this.label = label;
    this.taxableMinor = taxableMinor;
    this.taxMinor = taxMinor;
    this.amountMinor = amountMinor;
  }

  public String getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public long getTaxableMinor() {
    return taxableMinor;
  }

  public long getTaxMinor() {
    return taxMinor;
  }

  public long getAmountMinor() {
    return amountMinor;
  }
}
