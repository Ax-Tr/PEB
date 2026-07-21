package com.paywithease.employee.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Compensation structure for an employee. Amounts are integer paise ({@code *_minor}). One current
 * structure per employee. Statutory calculations (PF/ESI/PT) are computed in a later sprint; here
 * we only capture applicability flags and the salary components.
 */
@Entity
@Table(name = "salary_structures")
public class SalaryStructure {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "employee_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String employeeId;

  @Column(name = "gross_salary_minor", nullable = false)
  private long grossSalaryMinor;

  @Column(name = "basic_minor", nullable = false)
  private long basicMinor;

  @Column(name = "hra_minor", nullable = false)
  private long hraMinor;

  @Column(name = "pf_applicable", nullable = false)
  private boolean pfApplicable;

  @Column(name = "esi_applicable", nullable = false)
  private boolean esiApplicable;

  @Column(name = "pt_applicable", nullable = false)
  private boolean ptApplicable;

  @Column(name = "effective_from")
  private LocalDate effectiveFrom;

  @Version private long version;

  protected SalaryStructure() {}

  public SalaryStructure(String id, String tenantId, String employeeId) {
    this.id = id;
    this.tenantId = tenantId;
    this.employeeId = employeeId;
  }

  public void update(
      long grossSalaryMinor,
      long basicMinor,
      long hraMinor,
      boolean pfApplicable,
      boolean esiApplicable,
      boolean ptApplicable,
      LocalDate effectiveFrom) {
    this.grossSalaryMinor = grossSalaryMinor;
    this.basicMinor = basicMinor;
    this.hraMinor = hraMinor;
    this.pfApplicable = pfApplicable;
    this.esiApplicable = esiApplicable;
    this.ptApplicable = ptApplicable;
    this.effectiveFrom = effectiveFrom;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getEmployeeId() {
    return employeeId;
  }

  public long getGrossSalaryMinor() {
    return grossSalaryMinor;
  }

  public long getBasicMinor() {
    return basicMinor;
  }

  public long getHraMinor() {
    return hraMinor;
  }

  public boolean isPfApplicable() {
    return pfApplicable;
  }

  public boolean isEsiApplicable() {
    return esiApplicable;
  }

  public boolean isPtApplicable() {
    return ptApplicable;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }
}
