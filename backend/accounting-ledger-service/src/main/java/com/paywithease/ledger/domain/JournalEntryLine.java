package com.paywithease.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One debit or credit line of a journal entry (never both sides on one line). */
@Entity
@Table(name = "journal_entry_lines")
public class JournalEntryLine {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "journal_entry_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String journalEntryId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "account_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String accountId;

  @Column(name = "account_code", nullable = false)
  private String accountCode;

  @Column(name = "debit_minor", nullable = false)
  private long debitMinor;

  @Column(name = "credit_minor", nullable = false)
  private long creditMinor;

  @Column(name = "line_narration")
  private String lineNarration;

  protected JournalEntryLine() {}

  public JournalEntryLine(
      String id,
      String tenantId,
      String journalEntryId,
      String accountId,
      String accountCode,
      long debitMinor,
      long creditMinor,
      String lineNarration) {
    if (debitMinor < 0 || creditMinor < 0) {
      throw new IllegalArgumentException("Line amounts must be non-negative");
    }
    if (debitMinor > 0 && creditMinor > 0) {
      throw new IllegalArgumentException("A line cannot be both a debit and a credit");
    }
    this.id = id;
    this.tenantId = tenantId;
    this.journalEntryId = journalEntryId;
    this.accountId = accountId;
    this.accountCode = accountCode;
    this.debitMinor = debitMinor;
    this.creditMinor = creditMinor;
    this.lineNarration = lineNarration;
  }

  public String getId() {
    return id;
  }

  public String getJournalEntryId() {
    return journalEntryId;
  }

  public String getAccountId() {
    return accountId;
  }

  public String getAccountCode() {
    return accountCode;
  }

  public long getDebitMinor() {
    return debitMinor;
  }

  public long getCreditMinor() {
    return creditMinor;
  }
}
