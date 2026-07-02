package com.paywithease.ingestion.domain;

import com.paywithease.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A business bank account whose statements are imported for monitoring/reconciliation. */
@Entity
@Table(name = "bank_accounts")
public class BankAccount {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(name = "bank_name", nullable = false)
  private String bankName;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "account_number_enc", nullable = false)
  private String accountNumber;

  @Column(name = "account_number_hash", length = 64, nullable = false)
  private String accountNumberHash;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "ifsc_enc")
  private String ifsc;

  @Column(name = "account_type")
  private String accountType;

  @Column(name = "opening_balance_minor", nullable = false)
  private long openingBalanceMinor;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected BankAccount() {}

  public BankAccount(
      String id,
      String tenantId,
      String bankName,
      String accountNumber,
      String accountNumberHash,
      String ifsc,
      String accountType,
      long openingBalanceMinor,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.bankName = bankName;
    this.accountNumber = accountNumber;
    this.accountNumberHash = accountNumberHash;
    this.ifsc = ifsc;
    this.accountType = accountType;
    this.openingBalanceMinor = openingBalanceMinor;
    this.currency = "INR";
    this.createdAt = now;
  }

  public String getId() {
    return id;
  }

  public String getBankName() {
    return bankName;
  }

  public String getAccountType() {
    return accountType;
  }

  public long getOpeningBalanceMinor() {
    return openingBalanceMinor;
  }

  /** Last 4 digits for display (never expose the full number). */
  public String maskedAccountNumber() {
    String a = accountNumber == null ? "" : accountNumber;
    return a.length() <= 4 ? "****" : "****" + a.substring(a.length() - 4);
  }
}
