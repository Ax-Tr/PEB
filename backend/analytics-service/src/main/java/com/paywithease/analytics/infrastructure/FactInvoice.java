package com.paywithease.analytics.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** Revenue + receivable projection (one row per invoice), fed from {@code INVOICE_GENERATED}. */
@Entity
@Table(name = "fact_invoices")
public class FactInvoice {

  @Id
  @Column(name = "invoice_id", length = 26)
  private String invoiceId;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(name = "invoice_number")
  private String invoiceNumber;

  @Column(name = "customer_id", length = 26)
  private String customerId;

  @Column(name = "branch_id", length = 26)
  private String branchId;

  @Column(name = "invoice_date", nullable = false)
  private LocalDate invoiceDate;

  @Column(name = "period_year", nullable = false)
  private int periodYear;

  @Column(name = "period_month", nullable = false)
  private int periodMonth;

  @Column(name = "supply_type")
  private String supplyType;

  @Column(name = "taxable_minor", nullable = false)
  private long taxableMinor;

  @Column(name = "tax_minor", nullable = false)
  private long taxMinor;

  @Column(name = "total_minor", nullable = false)
  private long totalMinor;

  @Column(name = "amount_paid_minor", nullable = false)
  private long amountPaidMinor;

  @Column(nullable = false)
  private String status;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected FactInvoice() {}

  public FactInvoice(
      String invoiceId,
      String tenantId,
      String invoiceNumber,
      String customerId,
      String branchId,
      LocalDate invoiceDate,
      String supplyType,
      long taxableMinor,
      long taxMinor,
      long totalMinor,
      Instant now) {
    this.invoiceId = invoiceId;
    this.tenantId = tenantId;
    this.invoiceNumber = invoiceNumber;
    this.customerId = customerId;
    this.branchId = branchId;
    this.invoiceDate = invoiceDate;
    this.periodYear = invoiceDate.getYear();
    this.periodMonth = invoiceDate.getMonthValue();
    this.supplyType = supplyType;
    this.taxableMinor = taxableMinor;
    this.taxMinor = taxMinor;
    this.totalMinor = totalMinor;
    this.amountPaidMinor = 0;
    this.status = "ISSUED";
    this.updatedAt = now;
  }

  public long outstandingMinor() {
    return Math.max(0, totalMinor - amountPaidMinor);
  }

  public String getInvoiceId() {
    return invoiceId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getCustomerId() {
    return customerId;
  }

  public LocalDate getInvoiceDate() {
    return invoiceDate;
  }

  public long getTaxableMinor() {
    return taxableMinor;
  }

  public long getTaxMinor() {
    return taxMinor;
  }

  public long getTotalMinor() {
    return totalMinor;
  }

  public long getAmountPaidMinor() {
    return amountPaidMinor;
  }
}
