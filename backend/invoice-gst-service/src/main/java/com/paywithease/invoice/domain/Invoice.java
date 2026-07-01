package com.paywithease.invoice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A GST document (tax invoice, bill of supply, receipt voucher, credit/debit note). All document
 * types share this table keyed by {@code document_type}; notes reference the original via {@code
 * original_document_id}. Amounts are integer paise.
 */
@Entity
@Table(name = "invoices")
public class Invoice {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(name = "document_type", nullable = false)
  private String documentType;

  @Column(name = "supply_type", nullable = false)
  private String supplyType;

  @Column(name = "customer_id", length = 26)
  private String customerId;

  @Column(name = "customer_name")
  private String customerName;

  @Column(name = "customer_gstin")
  private String customerGstin;

  @Column(name = "place_of_supply", length = 2, nullable = false)
  private String placeOfSupply;

  @Column(name = "business_state_code", length = 2, nullable = false)
  private String businessStateCode;

  @Column(name = "invoice_number", nullable = false)
  private String invoiceNumber;

  @Column(name = "financial_year", nullable = false)
  private String financialYear;

  @Column(name = "invoice_date", nullable = false)
  private LocalDate invoiceDate;

  @Column(name = "original_document_id", length = 26)
  private String originalDocumentId;

  @Column(name = "reason")
  private String reason;

  @Column(name = "reverse_charge", nullable = false)
  private boolean reverseCharge;

  @Column(name = "taxable", nullable = false)
  private boolean taxable;

  @Column(name = "total_taxable_minor", nullable = false)
  private long totalTaxableMinor;

  @Column(name = "total_cgst_minor", nullable = false)
  private long totalCgstMinor;

  @Column(name = "total_sgst_minor", nullable = false)
  private long totalSgstMinor;

  @Column(name = "total_igst_minor", nullable = false)
  private long totalIgstMinor;

  @Column(name = "total_tax_minor", nullable = false)
  private long totalTaxMinor;

  @Column(name = "total_amount_minor", nullable = false)
  private long totalAmountMinor;

  @Column(name = "status", nullable = false)
  private String status = "ISSUED";

  @Column(name = "payment_request_id", length = 26)
  private String paymentRequestId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  protected Invoice() {}

  public Invoice(
      String id,
      String tenantId,
      String documentType,
      String supplyType,
      String customerId,
      String customerName,
      String customerGstin,
      String placeOfSupply,
      String businessStateCode,
      String invoiceNumber,
      String financialYear,
      LocalDate invoiceDate,
      String originalDocumentId,
      String reason,
      boolean reverseCharge,
      boolean taxable,
      long totalTaxableMinor,
      long totalCgstMinor,
      long totalSgstMinor,
      long totalIgstMinor,
      long totalTaxMinor,
      long totalAmountMinor,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.documentType = documentType;
    this.supplyType = supplyType;
    this.customerId = customerId;
    this.customerName = customerName;
    this.customerGstin = customerGstin;
    this.placeOfSupply = placeOfSupply;
    this.businessStateCode = businessStateCode;
    this.invoiceNumber = invoiceNumber;
    this.financialYear = financialYear;
    this.invoiceDate = invoiceDate;
    this.originalDocumentId = originalDocumentId;
    this.reason = reason;
    this.reverseCharge = reverseCharge;
    this.taxable = taxable;
    this.totalTaxableMinor = totalTaxableMinor;
    this.totalCgstMinor = totalCgstMinor;
    this.totalSgstMinor = totalSgstMinor;
    this.totalIgstMinor = totalIgstMinor;
    this.totalTaxMinor = totalTaxMinor;
    this.totalAmountMinor = totalAmountMinor;
    this.status = "ISSUED";
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void markSent(Instant now) {
    this.status = "SENT";
    this.updatedAt = now;
  }

  public void cancel(Instant now) {
    this.status = "CANCELLED";
    this.updatedAt = now;
  }

  public void linkPaymentRequest(String paymentRequestId, Instant now) {
    this.paymentRequestId = paymentRequestId;
    this.updatedAt = now;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getDocumentType() {
    return documentType;
  }

  public String getSupplyType() {
    return supplyType;
  }

  public String getCustomerId() {
    return customerId;
  }

  public String getCustomerName() {
    return customerName;
  }

  public String getCustomerGstin() {
    return customerGstin;
  }

  public String getPlaceOfSupply() {
    return placeOfSupply;
  }

  public String getBusinessStateCode() {
    return businessStateCode;
  }

  public String getInvoiceNumber() {
    return invoiceNumber;
  }

  public String getFinancialYear() {
    return financialYear;
  }

  public LocalDate getInvoiceDate() {
    return invoiceDate;
  }

  public String getOriginalDocumentId() {
    return originalDocumentId;
  }

  public String getReason() {
    return reason;
  }

  public boolean isReverseCharge() {
    return reverseCharge;
  }

  public boolean isTaxable() {
    return taxable;
  }

  public long getTotalTaxableMinor() {
    return totalTaxableMinor;
  }

  public long getTotalCgstMinor() {
    return totalCgstMinor;
  }

  public long getTotalSgstMinor() {
    return totalSgstMinor;
  }

  public long getTotalIgstMinor() {
    return totalIgstMinor;
  }

  public long getTotalTaxMinor() {
    return totalTaxMinor;
  }

  public long getTotalAmountMinor() {
    return totalAmountMinor;
  }

  public String getStatus() {
    return status;
  }

  public String getPaymentRequestId() {
    return paymentRequestId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getVersion() {
    return version;
  }
}
