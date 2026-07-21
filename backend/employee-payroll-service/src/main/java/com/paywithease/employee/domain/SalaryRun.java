package com.paywithease.employee.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A monthly payroll run for a business. Unique per (tenant, year, month) — cannot be double-run.
 */
@Entity
@Table(name = "salary_runs")
public class SalaryRun {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @Column(nullable = false)
  private int year;

  @Column(nullable = false)
  private int month;

  @Column(name = "working_days", nullable = false)
  private int workingDays;

  @Column(nullable = false)
  private String status;

  @Column(name = "total_earnings_minor", nullable = false)
  private long totalEarningsMinor;

  @Column(name = "total_net_minor", nullable = false)
  private long totalNetMinor;

  @Column(name = "total_statutory_minor", nullable = false)
  private long totalStatutoryMinor;

  @Column(name = "total_tds_minor", nullable = false)
  private long totalTdsMinor;

  @Column(name = "employee_count", nullable = false)
  private int employeeCount;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "created_by", length = 26, columnDefinition = "char(26)")
  private String createdBy;

  @Version private long version;

  protected SalaryRun() {}

  public SalaryRun(
      String id,
      String tenantId,
      int year,
      int month,
      int workingDays,
      String createdBy,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.year = year;
    this.month = month;
    this.workingDays = workingDays;
    this.status = "PROCESSED";
    this.createdBy = createdBy;
    this.createdAt = now;
  }

  public void addTotals(long earnings, long net, long statutory, long tds) {
    this.totalEarningsMinor += earnings;
    this.totalNetMinor += net;
    this.totalStatutoryMinor += statutory;
    this.totalTdsMinor += tds;
    this.employeeCount++;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public int getYear() {
    return year;
  }

  public int getMonth() {
    return month;
  }

  public int getWorkingDays() {
    return workingDays;
  }

  public String getStatus() {
    return status;
  }

  public long getTotalEarningsMinor() {
    return totalEarningsMinor;
  }

  public long getTotalNetMinor() {
    return totalNetMinor;
  }

  public long getTotalStatutoryMinor() {
    return totalStatutoryMinor;
  }

  public long getTotalTdsMinor() {
    return totalTdsMinor;
  }

  public int getEmployeeCount() {
    return employeeCount;
  }
}
