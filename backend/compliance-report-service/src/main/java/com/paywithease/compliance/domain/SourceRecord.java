package com.paywithease.compliance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A period read-model row built from an upstream event (a sale, purchase, or payroll run). */
@Entity
@Table(name = "source_records")
public class SourceRecord {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @Column(name = "record_type", nullable = false)
  private String recordType; // SALES, PURCHASE, PAYROLL

  @Column(nullable = false)
  private int year;

  @Column(nullable = false)
  private int month;

  @Column(name = "taxable_minor", nullable = false)
  private long taxableMinor;

  @Column(name = "tax_minor", nullable = false)
  private long taxMinor;

  @Column(name = "statutory_minor", nullable = false)
  private long statutoryMinor;

  @Column(name = "tds_minor", nullable = false)
  private long tdsMinor;

  @Column(name = "supply_type")
  private String supplyType;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "source_ref", length = 26, nullable = false, columnDefinition = "char(26)")
  private String sourceRef;

  private String reference;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected SourceRecord() {}

  public SourceRecord(
      String id,
      String tenantId,
      String recordType,
      int year,
      int month,
      long taxableMinor,
      long taxMinor,
      long statutoryMinor,
      long tdsMinor,
      String supplyType,
      String sourceRef,
      String reference,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.recordType = recordType;
    this.year = year;
    this.month = month;
    this.taxableMinor = taxableMinor;
    this.taxMinor = taxMinor;
    this.statutoryMinor = statutoryMinor;
    this.tdsMinor = tdsMinor;
    this.supplyType = supplyType;
    this.sourceRef = sourceRef;
    this.reference = reference;
    this.createdAt = now;
  }

  public String getId() {
    return id;
  }

  public String getRecordType() {
    return recordType;
  }

  public long getTaxableMinor() {
    return taxableMinor;
  }

  public long getTaxMinor() {
    return taxMinor;
  }

  public long getStatutoryMinor() {
    return statutoryMinor;
  }

  public long getTdsMinor() {
    return tdsMinor;
  }

  public String getSupplyType() {
    return supplyType;
  }
}
