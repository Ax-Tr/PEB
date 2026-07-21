package com.paywithease.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Running debit/credit totals per account (a read model kept in step with journal lines).
 * Optimistically locked so concurrent posts to the same account are serialized safely.
 */
@Entity
@Table(name = "ledger_balances")
public class LedgerBalance {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "account_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String accountId;

  @Column(name = "account_code", nullable = false)
  private String accountCode;

  @Column(name = "debit_total_minor", nullable = false)
  private long debitTotalMinor;

  @Column(name = "credit_total_minor", nullable = false)
  private long creditTotalMinor;

  @Version private long version;

  protected LedgerBalance() {}

  public LedgerBalance(String id, String tenantId, String accountId, String accountCode) {
    this.id = id;
    this.tenantId = tenantId;
    this.accountId = accountId;
    this.accountCode = accountCode;
  }

  public void addDebit(long minor) {
    this.debitTotalMinor += minor;
  }

  public void addCredit(long minor) {
    this.creditTotalMinor += minor;
  }

  /** Signed net balance on the account's normal side (positive = normal balance). */
  public long netBalanceMinor(NormalSide normalSide) {
    return normalSide == NormalSide.DEBIT
        ? debitTotalMinor - creditTotalMinor
        : creditTotalMinor - debitTotalMinor;
  }

  public String getId() {
    return id;
  }

  public String getAccountId() {
    return accountId;
  }

  public String getAccountCode() {
    return accountCode;
  }

  public long getDebitTotalMinor() {
    return debitTotalMinor;
  }

  public long getCreditTotalMinor() {
    return creditTotalMinor;
  }
}
