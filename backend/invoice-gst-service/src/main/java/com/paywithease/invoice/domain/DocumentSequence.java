package com.paywithease.invoice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Per-tenant, per-financial-year document number sequence. The composite primary key is (tenantId,
 * docType, financialYear); allocation is made concurrency-safe by pessimistically locking the row.
 */
@Entity
@Table(name = "document_sequences")
@IdClass(DocumentSequence.PK.class)
public class DocumentSequence {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @Id
  @Column(name = "doc_type", nullable = false)
  private String docType;

  @Id
  @Column(name = "financial_year", nullable = false)
  private String financialYear;

  @Column(name = "prefix", nullable = false)
  private String prefix;

  @Column(name = "next_number", nullable = false)
  private long nextNumber;

  protected DocumentSequence() {}

  public DocumentSequence(
      String tenantId, String docType, String financialYear, String prefix, long nextNumber) {
    this.tenantId = tenantId;
    this.docType = docType;
    this.financialYear = financialYear;
    this.prefix = prefix;
    this.nextNumber = nextNumber;
  }

  /** Returns the current number then advances the sequence by one. */
  public long allocate() {
    long current = nextNumber;
    nextNumber = current + 1;
    return current;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getDocType() {
    return docType;
  }

  public String getFinancialYear() {
    return financialYear;
  }

  public String getPrefix() {
    return prefix;
  }

  public long getNextNumber() {
    return nextNumber;
  }

  /** Composite primary key for {@link DocumentSequence}. */
  public static class PK implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tenantId;
    private String docType;
    private String financialYear;

    public PK() {}

    public PK(String tenantId, String docType, String financialYear) {
      this.tenantId = tenantId;
      this.docType = docType;
      this.financialYear = financialYear;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof PK pk)) {
        return false;
      }
      return Objects.equals(tenantId, pk.tenantId)
          && Objects.equals(docType, pk.docType)
          && Objects.equals(financialYear, pk.financialYear);
    }

    @Override
    public int hashCode() {
      return Objects.hash(tenantId, docType, financialYear);
    }
  }
}
