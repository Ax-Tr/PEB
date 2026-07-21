package com.paywithease.business.vendor.domain;

import com.paywithease.common.security.EncryptedStringConverter;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * A vendor's bank account for payouts. Account number and IFSC/UPI are encrypted at rest. Created
 * PENDING_REVIEW; becomes usable only once a user confirms it (product rule #7).
 */
@Entity
@Table(name = "vendor_bank_accounts")
public class VendorBankAccount {

  @Id
  @Column(columnDefinition = "bpchar", length = 26)
  private String id;

  @Column(name = "tenant_id", columnDefinition = "bpchar", length = 26, nullable = false)
  private String tenantId;

  @Column(name = "vendor_id", columnDefinition = "bpchar", length = 26, nullable = false)
  private String vendorId;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "account_number_enc", nullable = false)
  private String accountNumber;

  @Column(name = "account_number_hash", columnDefinition = "bpchar", length = 64, nullable = false)
  private String accountNumberHash;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "ifsc_enc", nullable = false)
  private String ifsc;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "upi_enc")
  private String upi;

  @Column(name = "bank_name", nullable = false)
  private String bankName;

  @Column(name = "holder_name", nullable = false)
  private String holderName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BankAccountStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private BankAccountSource source;

  @Column(name = "reviewed_by", columnDefinition = "bpchar", length = 26)
  private String reviewedBy;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected VendorBankAccount() {}

  public VendorBankAccount(
      String id,
      String tenantId,
      String vendorId,
      String accountNumber,
      String accountNumberHash,
      String ifsc,
      String upi,
      String bankName,
      String holderName,
      BankAccountSource source,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.vendorId = vendorId;
    this.accountNumber = accountNumber;
    this.accountNumberHash = accountNumberHash;
    this.ifsc = ifsc;
    this.upi = upi;
    this.bankName = bankName;
    this.holderName = holderName;
    this.source = source;
    this.status = BankAccountStatus.PENDING_REVIEW;
    this.createdAt = now;
  }

  public void confirm(String reviewerId, Instant now) {
    this.status = BankAccountStatus.VERIFIED;
    this.reviewedBy = reviewerId;
    this.reviewedAt = now;
  }

  public void reject(String reviewerId, Instant now) {
    this.status = BankAccountStatus.REJECTED;
    this.reviewedBy = reviewerId;
    this.reviewedAt = now;
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

  public String getAccountNumber() {
    return accountNumber;
  }

  public String getAccountNumberHash() {
    return accountNumberHash;
  }

  public String getIfsc() {
    return ifsc;
  }

  public String getUpi() {
    return upi;
  }

  public String getBankName() {
    return bankName;
  }

  public String getHolderName() {
    return holderName;
  }

  public BankAccountStatus getStatus() {
    return status;
  }

  public BankAccountSource getSource() {
    return source;
  }

  public String getReviewedBy() {
    return reviewedBy;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
