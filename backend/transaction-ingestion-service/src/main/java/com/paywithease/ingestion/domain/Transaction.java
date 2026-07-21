package com.paywithease.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A captured cash/bank/UPI/settlement transaction with its classification + reconciliation state.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "bank_account_id", length = 26, columnDefinition = "char(26)")
  private String bankAccountId;

  @Column(nullable = false)
  private String source;

  @Column(nullable = false)
  private String direction;

  @Column(name = "amount_minor", nullable = false)
  private long amountMinor;

  @Column(name = "txn_date", nullable = false)
  private LocalDate txnDate;

  private String narration;

  @Column(name = "external_ref")
  private String externalRef;

  private String counterparty;

  @Column(name = "dedupe_hash", length = 64, nullable = false)
  private String dedupeHash;

  private String category;

  @Column(name = "classification_status", nullable = false)
  private String classificationStatus;

  @Column(name = "classification_confidence", nullable = false)
  private BigDecimal classificationConfidence;

  @Column(nullable = false)
  private boolean reconciled;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "import_batch_id", length = 26, columnDefinition = "char(26)")
  private String importBatchId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Transaction() {}

  public Transaction(
      String id,
      String tenantId,
      String bankAccountId,
      TxnSource source,
      Direction direction,
      long amountMinor,
      LocalDate txnDate,
      String narration,
      String externalRef,
      String counterparty,
      String dedupeHash,
      String importBatchId,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.bankAccountId = bankAccountId;
    this.source = source.name();
    this.direction = direction.name();
    this.amountMinor = amountMinor;
    this.txnDate = txnDate;
    this.narration = narration;
    this.externalRef = externalRef;
    this.counterparty = counterparty;
    this.dedupeHash = dedupeHash;
    this.importBatchId = importBatchId;
    this.classificationStatus = ClassificationStatus.UNCLASSIFIED.name();
    this.classificationConfidence = BigDecimal.ZERO;
    this.reconciled = false;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public void applySuggestion(
      String category, BigDecimal confidence, boolean autoConfirm, Instant now) {
    this.category = category;
    this.classificationConfidence = confidence;
    this.classificationStatus =
        (autoConfirm ? ClassificationStatus.CONFIRMED : ClassificationStatus.SUGGESTED).name();
    this.updatedAt = now;
  }

  /** User confirms/overrides the category — the record is trusted from here. */
  public void confirm(String category, Instant now) {
    if (category != null && !category.isBlank()) {
      this.category = category;
    }
    this.classificationStatus = ClassificationStatus.CONFIRMED.name();
    this.updatedAt = now;
  }

  public void markReconciled(Instant now) {
    this.reconciled = true;
    this.updatedAt = now;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getBankAccountId() {
    return bankAccountId;
  }

  public String getSource() {
    return source;
  }

  public String getDirection() {
    return direction;
  }

  public long getAmountMinor() {
    return amountMinor;
  }

  public LocalDate getTxnDate() {
    return txnDate;
  }

  public String getNarration() {
    return narration;
  }

  public String getExternalRef() {
    return externalRef;
  }

  public String getCategory() {
    return category;
  }

  public String getClassificationStatus() {
    return classificationStatus;
  }

  public BigDecimal getClassificationConfidence() {
    return classificationConfidence;
  }

  public boolean isReconciled() {
    return reconciled;
  }
}
