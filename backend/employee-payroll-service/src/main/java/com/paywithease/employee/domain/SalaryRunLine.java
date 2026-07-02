package com.paywithease.employee.domain;

import com.paywithease.employee.domain.payroll.PayrollCalculator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One employee's computed payslip line within a salary run. */
@Entity
@Table(name = "salary_run_lines")
public class SalaryRunLine {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(name = "salary_run_id", length = 26, nullable = false)
  private String salaryRunId;

  @Column(name = "employee_id", length = 26, nullable = false)
  private String employeeId;

  @Column(name = "gross_minor", nullable = false)
  private long grossMinor;

  @Column(name = "basic_minor", nullable = false)
  private long basicMinor;

  @Column(name = "lop_days", nullable = false)
  private int lopDays;

  @Column(name = "earned_gross_minor", nullable = false)
  private long earnedGrossMinor;

  @Column(name = "incentives_minor", nullable = false)
  private long incentivesMinor;

  @Column(name = "pf_minor", nullable = false)
  private long pfMinor;

  @Column(name = "esi_minor", nullable = false)
  private long esiMinor;

  @Column(name = "pt_minor", nullable = false)
  private long ptMinor;

  @Column(name = "tds_minor", nullable = false)
  private long tdsMinor;

  @Column(name = "other_deductions_minor", nullable = false)
  private long otherDeductionsMinor;

  @Column(name = "net_pay_minor", nullable = false)
  private long netPayMinor;

  @Column(name = "payslip_document_id", length = 26)
  private String payslipDocumentId;

  protected SalaryRunLine() {}

  public SalaryRunLine(
      String id,
      String tenantId,
      String salaryRunId,
      String employeeId,
      long grossMinor,
      long basicMinor,
      int lopDays,
      PayrollCalculator.Result r) {
    this.id = id;
    this.tenantId = tenantId;
    this.salaryRunId = salaryRunId;
    this.employeeId = employeeId;
    this.grossMinor = grossMinor;
    this.basicMinor = basicMinor;
    this.lopDays = lopDays;
    this.earnedGrossMinor = r.earnedGrossMinor();
    this.incentivesMinor = r.incentivesMinor();
    this.pfMinor = r.pfMinor();
    this.esiMinor = r.esiMinor();
    this.ptMinor = r.ptMinor();
    this.tdsMinor = r.tdsMinor();
    this.otherDeductionsMinor = r.otherDeductionsMinor();
    this.netPayMinor = r.netPayMinor();
  }

  public void attachPayslip(String documentId) {
    this.payslipDocumentId = documentId;
  }

  public String getId() {
    return id;
  }

  public String getEmployeeId() {
    return employeeId;
  }

  public long getGrossMinor() {
    return grossMinor;
  }

  public long getBasicMinor() {
    return basicMinor;
  }

  public int getLopDays() {
    return lopDays;
  }

  public long getEarnedGrossMinor() {
    return earnedGrossMinor;
  }

  public long getIncentivesMinor() {
    return incentivesMinor;
  }

  public long getPfMinor() {
    return pfMinor;
  }

  public long getEsiMinor() {
    return esiMinor;
  }

  public long getPtMinor() {
    return ptMinor;
  }

  public long getTdsMinor() {
    return tdsMinor;
  }

  public long getOtherDeductionsMinor() {
    return otherDeductionsMinor;
  }

  public long getNetPayMinor() {
    return netPayMinor;
  }

  public String getPayslipDocumentId() {
    return payslipDocumentId;
  }
}
