package com.paywithease.common.idempotency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Durable idempotency record for money/ledger mutations. Unique on {@code (tenantId, key)}; a
 * replay with the same request hash returns the stored response, a different hash is rejected (see
 * specs/idempotency-outbox-saga.md).
 */
@Entity
@Table(name = "idempotency_keys")
@IdClass(IdempotencyKey.PK.class)
public class IdempotencyKey {

  public enum Status {
    PROCESSING,
    COMPLETED,
    FAILED
  }

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", columnDefinition = "char(26)", nullable = false)
  private String tenantId;

  @Id
  @Column(name = "key", nullable = false)
  private String key;

  @Column(name = "endpoint", nullable = false)
  private String endpoint;

  @Column(name = "request_hash", nullable = false)
  private String requestHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status;

  @Column(name = "response", columnDefinition = "jsonb")
  private String response;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected IdempotencyKey() {}

  public IdempotencyKey(
      String tenantId, String key, String endpoint, String requestHash, Instant createdAt) {
    this.tenantId = tenantId;
    this.key = key;
    this.endpoint = endpoint;
    this.requestHash = requestHash;
    this.status = Status.PROCESSING;
    this.createdAt = createdAt;
  }

  public void complete(String response) {
    this.status = Status.COMPLETED;
    this.response = response;
  }

  public void fail() {
    this.status = Status.FAILED;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getKey() {
    return key;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public String getRequestHash() {
    return requestHash;
  }

  public Status getStatus() {
    return status;
  }

  public String getResponse() {
    return response;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Composite primary key. */
  public static class PK implements Serializable {
    private static final long serialVersionUID = 1L;
    private String tenantId;
    private String key;

    public PK() {}

    public PK(String tenantId, String key) {
      this.tenantId = tenantId;
      this.key = key;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PK pk)) return false;
      return Objects.equals(tenantId, pk.tenantId) && Objects.equals(key, pk.key);
    }

    @Override
    public int hashCode() {
      return Objects.hash(tenantId, key);
    }
  }
}
