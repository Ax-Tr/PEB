package com.paywithease.vendor.domain;

import com.paywithease.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A vendor's bank account for payouts. Account number and IFSC/UPI are encrypted at rest; a blind
 * index (HMAC) of the account number allows de-duplication without exposing plaintext.
 *
 * <p>Security gate (product rule #7): a bank account is created {@code PENDING_REVIEW} and only
 * becomes usable once a user explicitly {@link #confirm(String, Instant) confirms} it. This is
 * critical for OCR-captured details, where mis-recognised digits must never silently reach payouts.
 */
@Entity
@Table(name = "vendor_bank_accounts")
public class VendorBankAccount {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(name = "vendor_id", length = 26, nullable = false)
  private String vendorId;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "account_number_enc", nullable = false)
  private String accountNumber;

  @Column(name = "account_number_hash", length = 64, nullable = false)
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

  @Column(name = "reviewed_by", length = 26)
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

  /** User-approves the reviewed details, making the account usable for payouts. */
  public void confirm(String reviewerId, Instant now) {
    this.status = BankAccountStatus.VERIFIED;
    this.reviewedBy = reviewerId;
    this.reviewedAt = now;
  }

  /** User-rejects the captured details (e.g. OCR mis-read); the account stays unusable. */
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
