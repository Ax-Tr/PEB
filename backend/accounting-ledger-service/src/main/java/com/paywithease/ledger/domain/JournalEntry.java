package com.paywithease.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A posted double-entry journal. Immutable once posted (no UPDATE/DELETE) — corrections are new
 * reversing entries. References its source event for traceability and idempotency.
 */
@Entity
@Table(name = "journal_entries")
public class JournalEntry {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(name = "entry_date", nullable = false)
  private LocalDate entryDate;

  private String narration;

  @Column(name = "source_service")
  private String sourceService;

  @Column(name = "source_event_id", length = 26)
  private String sourceEventId;

  @Column(name = "reversal_of", length = 26)
  private String reversalOf;

  @Column(name = "period_id", length = 26)
  private String periodId;

  @Column(nullable = false)
  private String status;

  @Column(name = "correlation_id")
  private String correlationId;

  @Column(name = "created_by", length = 26)
  private String createdBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected JournalEntry() {}

  public JournalEntry(
      String id,
      String tenantId,
      LocalDate entryDate,
      String narration,
      String sourceService,
      String sourceEventId,
      String reversalOf,
      String periodId,
      String correlationId,
      String createdBy,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.entryDate = entryDate;
    this.narration = narration;
    this.sourceService = sourceService;
    this.sourceEventId = sourceEventId;
    this.reversalOf = reversalOf;
    this.periodId = periodId;
    this.correlationId = correlationId;
    this.createdBy = createdBy;
    this.status = "POSTED";
    this.createdAt = now;
  }

  public void markReversed() {
    this.status = "REVERSED";
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public LocalDate getEntryDate() {
    return entryDate;
  }

  public String getNarration() {
    return narration;
  }

  public String getSourceService() {
    return sourceService;
  }

  public String getSourceEventId() {
    return sourceEventId;
  }

  public String getReversalOf() {
    return reversalOf;
  }

  public String getPeriodId() {
    return periodId;
  }

  public String getStatus() {
    return status;
  }
}
