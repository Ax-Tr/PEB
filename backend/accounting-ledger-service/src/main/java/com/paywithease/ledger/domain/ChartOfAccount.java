package com.paywithease.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A ledger account in a business's chart of accounts. */
@Entity
@Table(name = "chart_of_accounts")
public class ChartOfAccount {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @Column(nullable = false)
  private String code;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AccountType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "normal_side", nullable = false)
  private NormalSide normalSide;

  @Column(name = "is_contra", nullable = false)
  private boolean contra;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ChartOfAccount() {}

  public ChartOfAccount(
      String id,
      String tenantId,
      String code,
      String name,
      AccountType type,
      boolean contra,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.code = code;
    this.name = name;
    this.type = type;
    this.normalSide = type.normalSide();
    this.contra = contra;
    this.createdAt = now;
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public AccountType getType() {
    return type;
  }

  public NormalSide getNormalSide() {
    return normalSide;
  }

  public boolean isContra() {
    return contra;
  }
}
