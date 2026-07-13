package com.paywithease.common.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Transactional outbox row. Written in the SAME database transaction as the state change; a relay
 * publishes unpublished rows to Kafka and stamps {@code publishedAt}. Guarantees at-least-once
 * delivery with no dual-write inconsistency (see specs/idempotency-outbox-saga.md).
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(columnDefinition = "char(26)", nullable = false)
  private String id;

  @Column(name = "aggregate_type", nullable = false)
  private String aggregateType;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "aggregate_id", columnDefinition = "char(26)", nullable = false)
  private String aggregateId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "event_version", nullable = false)
  private int eventVersion;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", columnDefinition = "char(26)", nullable = false)
  private String tenantId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
  private String payload;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "headers", columnDefinition = "jsonb", nullable = false)
  private String headers;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  protected OutboxEvent() {}

  public OutboxEvent(
      String id,
      String aggregateType,
      String aggregateId,
      String eventType,
      int eventVersion,
      String tenantId,
      String payload,
      String headers,
      Instant createdAt) {
    this.id = id;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.eventVersion = eventVersion;
    this.tenantId = tenantId;
    this.payload = payload;
    this.headers = headers;
    this.createdAt = createdAt;
    this.attempts = 0;
  }

  public void markPublished(Instant when) {
    this.publishedAt = when;
  }

  public void incrementAttempts() {
    this.attempts++;
  }

  public String getId() {
    return id;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public String getAggregateId() {
    return aggregateId;
  }

  public String getEventType() {
    return eventType;
  }

  public int getEventVersion() {
    return eventVersion;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getPayload() {
    return payload;
  }

  public String getHeaders() {
    return headers;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public int getAttempts() {
    return attempts;
  }
}
