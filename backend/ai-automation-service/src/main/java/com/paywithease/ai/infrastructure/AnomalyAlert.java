package com.paywithease.ai.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** An anomaly alert raised by the detector; a human acknowledges or dismisses it. */
@Entity
@Table(name = "anomaly_alerts")
public class AnomalyAlert {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @Column(name = "subject_type", nullable = false)
  private String subjectType;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "subject_id", length = 26, columnDefinition = "char(26)")
  private String subjectId;

  @Column(nullable = false)
  private String metric;

  @Column(name = "observed_minor", nullable = false)
  private long observedMinor;

  @Column(nullable = false)
  private BigDecimal score;

  @Column(nullable = false)
  private String severity;

  @Column(nullable = false)
  private String status;

  @Column private String detail;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "acknowledged_by", length = 26, columnDefinition = "char(26)")
  private String acknowledgedBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Version private long version;

  protected AnomalyAlert() {}

  public AnomalyAlert(
      String id,
      String tenantId,
      String subjectType,
      String subjectId,
      String metric,
      long observedMinor,
      BigDecimal score,
      String severity,
      String detail,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.subjectType = subjectType;
    this.subjectId = subjectId;
    this.metric = metric;
    this.observedMinor = observedMinor;
    this.score = score;
    this.severity = severity;
    this.status = "OPEN";
    this.detail = detail;
    this.createdAt = now;
  }

  public void acknowledge(String actor) {
    this.status = "ACKNOWLEDGED";
    this.acknowledgedBy = actor;
  }

  public void dismiss(String actor) {
    this.status = "DISMISSED";
    this.acknowledgedBy = actor;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getSubjectType() {
    return subjectType;
  }

  public String getSubjectId() {
    return subjectId;
  }

  public long getObservedMinor() {
    return observedMinor;
  }

  public BigDecimal getScore() {
    return score;
  }

  public String getSeverity() {
    return severity;
  }

  public String getStatus() {
    return status;
  }

  public String getDetail() {
    return detail;
  }
}
