package com.paywithease.purchase.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A vendor purchase bill with the input GST (ITC) computed via the shared GST engine. Amounts are
 * integer paise.
 */
@Entity
@Table(name = "purchase_bills")
public class PurchaseBill {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(name = "vendor_id", length = 26)
  private String vendorId;

  @Column(name = "vendor_name")
  private String vendorName;

  @Column(name = "vendor_gstin")
  private String vendorGstin;

  @Column(name = "bill_number")
  private String billNumber;

  @Column(name = "bill_date", nullable = false)
  private LocalDate billDate;

  @Column(name = "place_of_supply", length = 2, nullable = false)
  private String placeOfSupply;

  @Column(name = "business_state_code", length = 2, nullable = false)
  private String businessStateCode;

  @Column(name = "reverse_charge", nullable = false)
  private boolean reverseCharge;

  @Column(name = "total_taxable_minor", nullable = false)
  private long totalTaxableMinor;

  @Column(name = "total_input_gst_minor", nullable = false)
  private long totalInputGstMinor;

  @Column(name = "total_cgst_minor", nullable = false)
  private long totalCgstMinor;

  @Column(name = "total_sgst_minor", nullable = false)
  private long totalSgstMinor;

  @Column(name = "total_igst_minor", nullable = false)
  private long totalIgstMinor;

  @Column(name = "total_amount_minor", nullable = false)
  private long totalAmountMinor;

  @Column(name = "status", nullable = false)
  private String status = "RECORDED";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  protected PurchaseBill() {}

  public PurchaseBill(
      String id,
      String tenantId,
      String vendorId,
      String vendorName,
      String vendorGstin,
      String billNumber,
      LocalDate billDate,
      String placeOfSupply,
      String businessStateCode,
      boolean reverseCharge,
      long totalTaxableMinor,
      long totalInputGstMinor,
      long totalCgstMinor,
      long totalSgstMinor,
      long totalIgstMinor,
      long totalAmountMinor,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.vendorId = vendorId;
    this.vendorName = vendorName;
    this.vendorGstin = vendorGstin;
    this.billNumber = billNumber;
    this.billDate = billDate;
    this.placeOfSupply = placeOfSupply;
    this.businessStateCode = businessStateCode;
    this.reverseCharge = reverseCharge;
    this.totalTaxableMinor = totalTaxableMinor;
    this.totalInputGstMinor = totalInputGstMinor;
    this.totalCgstMinor = totalCgstMinor;
    this.totalSgstMinor = totalSgstMinor;
    this.totalIgstMinor = totalIgstMinor;
    this.totalAmountMinor = totalAmountMinor;
    this.status = "RECORDED";
    this.createdAt = now;
    this.updatedAt = now;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getVendorId() {
    return vendorId;
  }

  public String getVendorName() {
    return vendorName;
  }

  public String getVendorGstin() {
    return vendorGstin;
  }

  public String getBillNumber() {
    return billNumber;
  }

  public LocalDate getBillDate() {
    return billDate;
  }

  public String getPlaceOfSupply() {
    return placeOfSupply;
  }

  public String getBusinessStateCode() {
    return businessStateCode;
  }

  public boolean isReverseCharge() {
    return reverseCharge;
  }

  public long getTotalTaxableMinor() {
    return totalTaxableMinor;
  }

  public long getTotalInputGstMinor() {
    return totalInputGstMinor;
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

  public long getTotalAmountMinor() {
    return totalAmountMinor;
  }

  public String getStatus() {
    return status;
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
