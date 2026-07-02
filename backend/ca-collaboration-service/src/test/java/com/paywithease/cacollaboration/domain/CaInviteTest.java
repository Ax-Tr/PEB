package com.paywithease.cacollaboration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paywithease.common.error.ApiException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CaInviteTest {

  private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

  private CaInvite invite(CollaboratorRole role, Instant expiry) {
    return new CaInvite("i1", "t1", "ca@example.com", role, "owner1", expiry, NOW);
  }

  @Test
  void acceptMovesPendingToAcceptedAndGrantsAccess() {
    CaInvite i = invite(CollaboratorRole.CA, NOW.plusSeconds(3600));
    i.accept("user9", NOW);
    assertThat(i.getStatus()).isEqualTo("ACCEPTED");
    assertThat(i.getLinkedUserId()).isEqualTo("user9");
    assertThat(i.hasActiveAccess(NOW)).isTrue();
  }

  @Test
  void acceptAfterExpiryFails() {
    CaInvite i = invite(CollaboratorRole.CA, NOW.minusSeconds(1));
    assertThatThrownBy(() -> i.accept("user9", NOW))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("expired");
  }

  @Test
  void revokeRemovesActiveAccessEvenAfterAcceptance() {
    CaInvite i = invite(CollaboratorRole.CA, NOW.plusSeconds(3600));
    i.accept("user9", NOW);
    i.revoke(NOW);
    assertThat(i.getStatus()).isEqualTo("REVOKED");
    assertThat(i.hasActiveAccess(NOW)).isFalse();
  }

  @Test
  void expiredByTimeHasNoActiveAccess() {
    CaInvite i = invite(CollaboratorRole.ACCOUNTANT, NOW.plusSeconds(10));
    i.accept("user9", NOW);
    assertThat(i.hasActiveAccess(NOW.plusSeconds(20))).isFalse();
  }
}
