package com.paywithease.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Grant of a role to a user (optionally scoped to a tenant/business). */
@Entity
@Table(name = "user_roles")
@IdClass(UserRole.PK.class)
public class UserRole {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "user_id", columnDefinition = "char(26)")
  private String userId;

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "role_id", columnDefinition = "char(26)")
  private String roleId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", columnDefinition = "char(26)")
  private String tenantId;

  @Column(name = "granted_at", nullable = false)
  private Instant grantedAt;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "granted_by", columnDefinition = "char(26)")
  private String grantedBy;

  protected UserRole() {}

  public UserRole(String userId, String roleId, String tenantId, String grantedBy, Instant now) {
    this.userId = userId;
    this.roleId = roleId;
    this.tenantId = tenantId;
    this.grantedBy = grantedBy;
    this.grantedAt = now;
  }

  public String getUserId() {
    return userId;
  }

  public String getRoleId() {
    return roleId;
  }

  public static class PK implements Serializable {
    private static final long serialVersionUID = 1L;
    private String userId;
    private String roleId;

    public PK() {}

    public PK(String userId, String roleId) {
      this.userId = userId;
      this.roleId = roleId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PK pk)) return false;
      return Objects.equals(userId, pk.userId) && Objects.equals(roleId, pk.roleId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(userId, roleId);
    }
  }
}
