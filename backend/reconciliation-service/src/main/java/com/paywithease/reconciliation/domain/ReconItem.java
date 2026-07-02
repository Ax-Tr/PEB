package com.paywithease.reconciliation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** A normalized reconcilable item from either side, matched at most once. */
@Entity
@Table(name = "recon_items")
public class ReconItem {

  @Id
  @Column(length = 26)
  private String id;

  @Column(name = "tenant_id", length = 26, nullable = false)
  private String tenantId;

  @Column(nullable = false)
  private String side;

  @Column(name = "source_type", nullable = false)
  private String sourceType;

  @Column(name = "source_ref", length = 26, nullable = false)
  private String sourceRef;

  @Column(nullable = false)
  private String direction;

  @Column(name = "amount_minor", nullable = false)
  private long amountMinor;

  @Column(name = "item_date", nullable = false)
  private LocalDate itemDate;

  private String reference;
  private String counterparty;
  private String narration;

  @Column(nullable = false)
  private boolean matched;

  @Column(name = "match_id", length = 26)
  private String matchId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ReconItem() {}

  public ReconItem(
      String id,
      String tenantId,
      ReconSide side,
      String sourceType,
      String sourceRef,
      String direction,
      long amountMinor,
      LocalDate itemDate,
      String reference,
      String counterparty,
      String narration,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.side = side.name();
    this.sourceType = sourceType;
    this.sourceRef = sourceRef;
    this.direction = direction;
    this.amountMinor = amountMinor;
    this.itemDate = itemDate;
    this.reference = reference;
    this.counterparty = counterparty;
    this.narration = narration;
    this.matched = false;
    this.createdAt = now;
  }

  public void markMatched(String matchId) {
    this.matched = true;
    this.matchId = matchId;
  }

  public void unmatch() {
    this.matched = false;
    this.matchId = null;
  }

  public MatchEngine.Item toMatchItem() {
    return new MatchEngine.Item(
        id, direction, amountMinor, itemDate, reference, counterparty, narration);
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getSide() {
    return side;
  }

  public String getSourceType() {
    return sourceType;
  }

  public String getSourceRef() {
    return sourceRef;
  }

  public String getDirection() {
    return direction;
  }

  public long getAmountMinor() {
    return amountMinor;
  }

  public LocalDate getItemDate() {
    return itemDate;
  }

  public boolean isMatched() {
    return matched;
  }

  public String getMatchId() {
    return matchId;
  }
}
