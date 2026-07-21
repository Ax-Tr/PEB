package com.paywithease.analytics.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Cost + payable projection (one row per purchase bill), fed from {@code PURCHASE_BILL_CREATED}.
 */
@Entity
@Table(name = "fact_purchases")
public class FactPurchase {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "bill_id", length = 26, columnDefinition = "char(26)")
  private String billId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "vendor_id", length = 26, columnDefinition = "char(26)")
  private String vendorId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "branch_id", length = 26, columnDefinition = "char(26)")
  private String branchId;

  @Column(name = "bill_date", nullable = false)
  private LocalDate billDate;

  @Column(name = "period_year", nullable = false)
  private int periodYear;

  @Column(name = "period_month", nullable = false)
  private int periodMonth;

  @Column(name = "net_minor", nullable = false)
  private long netMinor;

  @Column(name = "input_gst_minor", nullable = false)
  private long inputGstMinor;

  @Column(name = "total_minor", nullable = false)
  private long totalMinor;

  @Column(name = "amount_paid_minor", nullable = false)
  private long amountPaidMinor;

  @Column(nullable = false)
  private String status;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected FactPurchase() {}

  public FactPurchase(
      String billId,
      String tenantId,
      String vendorId,
      String branchId,
      LocalDate billDate,
      long netMinor,
      long inputGstMinor,
      long totalMinor,
      Instant now) {
    this.billId = billId;
    this.tenantId = tenantId;
    this.vendorId = vendorId;
    this.branchId = branchId;
    this.billDate = billDate;
    this.periodYear = billDate.getYear();
    this.periodMonth = billDate.getMonthValue();
    this.netMinor = netMinor;
    this.inputGstMinor = inputGstMinor;
    this.totalMinor = totalMinor;
    this.amountPaidMinor = 0;
    this.status = "RECORDED";
    this.updatedAt = now;
  }

  public long outstandingMinor() {
    return Math.max(0, totalMinor - amountPaidMinor);
  }

  public String getBillId() {
    return billId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getVendorId() {
    return vendorId;
  }

  public LocalDate getBillDate() {
    return billDate;
  }

  public long getNetMinor() {
    return netMinor;
  }

  public long getInputGstMinor() {
    return inputGstMinor;
  }

  public long getTotalMinor() {
    return totalMinor;
  }
}
