package com.paywithease.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Summary of one statement/feed import: how many rows were new vs. duplicates. */
@Entity
@Table(name = "import_batches")
public class ImportBatch {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(name = "bank_account_id", length = 26)
  private String bankAccountId;

  @Column(nullable = false)
  private String source;

  @Column(name = "file_name")
  private String fileName;

  @Column(name = "total_rows", nullable = false)
  private int totalRows;

  @Column(name = "imported_count", nullable = false)
  private int importedCount;

  @Column(name = "duplicate_count", nullable = false)
  private int duplicateCount;

  @Column(nullable = false)
  private String status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ImportBatch() {}

  public ImportBatch(
      String id,
      String tenantId,
      String bankAccountId,
      TxnSource source,
      String fileName,
      int totalRows,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.bankAccountId = bankAccountId;
    this.source = source.name();
    this.fileName = fileName;
    this.totalRows = totalRows;
    this.status = "COMPLETED";
    this.createdAt = now;
  }

  public void recordImported() {
    this.importedCount++;
  }

  public void recordDuplicate() {
    this.duplicateCount++;
  }

  public String getId() {
    return id;
  }

  public int getTotalRows() {
    return totalRows;
  }

  public int getImportedCount() {
    return importedCount;
  }

  public int getDuplicateCount() {
    return duplicateCount;
  }
}
