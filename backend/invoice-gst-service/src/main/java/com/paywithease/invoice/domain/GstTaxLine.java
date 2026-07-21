package com.paywithease.invoice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Tax grouped by GST rate for an invoice — the shape used for GSTR summaries. Amounts are paise.
 */
@Entity
@Table(name = "gst_tax_lines")
public class GstTaxLine {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "invoice_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String invoiceId;

  @Column(name = "gst_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal gstRate;

  @Column(name = "taxable_value_minor", nullable = false)
  private long taxableValueMinor;

  @Column(name = "cgst_minor", nullable = false)
  private long cgstMinor;

  @Column(name = "sgst_minor", nullable = false)
  private long sgstMinor;

  @Column(name = "igst_minor", nullable = false)
  private long igstMinor;

  protected GstTaxLine() {}

  public GstTaxLine(
      String id,
      String tenantId,
      String invoiceId,
      BigDecimal gstRate,
      long taxableValueMinor,
      long cgstMinor,
      long sgstMinor,
      long igstMinor) {
    this.id = id;
    this.tenantId = tenantId;
    this.invoiceId = invoiceId;
    this.gstRate = gstRate;
    this.taxableValueMinor = taxableValueMinor;
    this.cgstMinor = cgstMinor;
    this.sgstMinor = sgstMinor;
    this.igstMinor = igstMinor;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getInvoiceId() {
    return invoiceId;
  }

  public BigDecimal getGstRate() {
    return gstRate;
  }

  public long getTaxableValueMinor() {
    return taxableValueMinor;
  }

  public long getCgstMinor() {
    return cgstMinor;
  }

  public long getSgstMinor() {
    return sgstMinor;
  }

  public long getIgstMinor() {
    return igstMinor;
  }
}
