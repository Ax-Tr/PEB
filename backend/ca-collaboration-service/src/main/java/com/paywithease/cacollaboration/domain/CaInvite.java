package com.paywithease.cacollaboration.domain;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * An invitation for an external collaborator (accountant / CA / auditor). Access is governed by a
 * small state machine and can be REVOKED at any point — including mid-review — after which {@link
 * #hasActiveAccess(Instant)} returns false so the API can immediately block the collaborator.
 */
@Entity
@Table(name = "ca_invites")
public class CaInvite {

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 26, columnDefinition = "char(26)")
  private String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tenant_id", length = 26, nullable = false, columnDefinition = "char(26)")
  private String tenantId;

  @Convert(converter = EncryptedStringConverter.class)
  @Column(name = "email_enc", nullable = false)
  private String email;

  @Column(nullable = false)
  private String role;

  @Column(nullable = false)
  private String status;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "linked_user_id", length = 26, columnDefinition = "char(26)")
  private String linkedUserId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "invited_by", length = 26, nullable = false, columnDefinition = "char(26)")
  private String invitedBy;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "accepted_at")
  private Instant acceptedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Version private long version;

  protected CaInvite() {}

  public CaInvite(
      String id,
      String tenantId,
      String email,
      CollaboratorRole role,
      String invitedBy,
      Instant expiresAt,
      Instant now) {
    this.id = id;
    this.tenantId = tenantId;
    this.email = email;
    this.role = role.name();
    this.status = InviteStatus.PENDING.name();
    this.invitedBy = invitedBy;
    this.expiresAt = expiresAt;
    this.createdAt = now;
  }

  public void accept(String userId, Instant now) {
    if (!InviteStatus.PENDING.name().equals(status)) {
      throw new ApiException(
          ErrorCode.CONFLICT, "Only a pending invite can be accepted (current: " + status + ")");
    }
    if (now.isAfter(expiresAt)) {
      this.status = InviteStatus.EXPIRED.name();
      throw new ApiException(ErrorCode.CONFLICT, "Invitation has expired");
    }
    this.status = InviteStatus.ACCEPTED.name();
    this.linkedUserId = userId;
    this.acceptedAt = now;
  }

  /** Revoke access. Allowed from PENDING or ACCEPTED (i.e. even in the middle of a review). */
  public void revoke(Instant now) {
    if (InviteStatus.REVOKED.name().equals(status)) {
      return; // idempotent
    }
    this.status = InviteStatus.REVOKED.name();
    this.revokedAt = now;
  }

  public boolean hasActiveAccess(Instant now) {
    return InviteStatus.ACCEPTED.name().equals(status) && now.isBefore(expiresAt);
  }

  public String getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getEmail() {
    return email;
  }

  public String getRole() {
    return role;
  }

  public String getStatus() {
    return status;
  }

  public String getLinkedUserId() {
    return linkedUserId;
  }
}
