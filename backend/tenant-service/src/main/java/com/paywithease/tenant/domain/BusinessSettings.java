package com.paywithease.tenant.domain;

import com.paywithease.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/** Invoice/tax/display settings for a business. */
@Entity
@Table(name = "business_settings")
public class BusinessSettings {

  @Id
  @Column(name = "tenant_id", length = 26)
  private String tenantId;

  @Column(name = "invoice_prefix", nullable = false)
  private String invoicePrefix;

  @Column(name = "invoice_next_number", nullable = false)
  private long invoiceNextNumber;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "upi_id_enc")
  private String upiId;

  @Column(name = "logo_url")
  private String logoUrl;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "financial_year_start_month", nullable = false)
  private int financialYearStartMonth;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version private long version;

  protected BusinessSettings() {}

  public BusinessSettings(String tenantId, Instant now) {
    this.tenantId = tenantId;
    this.invoicePrefix = "INV";
    this.invoiceNextNumber = 1;
    this.currency = "INR";
    this.financialYearStartMonth = 4;
    this.updatedAt = now;
  }

  public void update(String invoicePrefix, String upiId, String logoUrl, Instant now) {
    if (invoicePrefix != null && !invoicePrefix.isBlank()) {
      this.invoicePrefix = invoicePrefix;
    }
    this.upiId = upiId;
    this.logoUrl = logoUrl;
    this.updatedAt = now;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getInvoicePrefix() {
    return invoicePrefix;
  }

  public long getInvoiceNextNumber() {
    return invoiceNextNumber;
  }

  public String getUpiId() {
    return upiId;
  }

  public String getLogoUrl() {
    return logoUrl;
  }

  public String getCurrency() {
    return currency;
  }

  public int getFinancialYearStartMonth() {
    return financialYearStartMonth;
  }
}
