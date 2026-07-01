package com.paywithease.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paywithease.common.error.ApiException;
import com.paywithease.identity.domain.UserSession;
import com.paywithease.identity.infrastructure.UserSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

  @Mock UserSessionRepository repo;
  private SessionService service;
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service = new SessionService(repo, clock, 30);
    // Lenient: not every test reaches a save (e.g. the unknown-token path throws first).
    lenient().when(repo.save(any(UserSession.class))).thenAnswer(i -> i.getArgument(0));
  }

  @Test
  void createIssuesActiveSessionWithOpaqueToken() {
    SessionService.Issued issued = service.create("user1", "device1", "1.2.3.4", "agent");
    assertThat(issued.refreshToken()).isNotBlank();
    ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
    verify(repo).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(UserSession.Status.ACTIVE);
    assertThat(captor.getValue().getExpiresAt()).isEqualTo(Instant.parse("2026-07-31T00:00:00Z"));
  }

  @Test
  void rotateReplacesActiveTokenInSameFamily() {
    UserSession active = activeSession("tok-1", "fam-1");
    when(repo.findByRefreshTokenHash(SessionService.hash("tok-1"))).thenReturn(Optional.of(active));

    SessionService.Issued rotated = service.rotate("tok-1", "1.2.3.4", "agent");
    assertThat(rotated.refreshToken()).isNotBlank().isNotEqualTo("tok-1");
    assertThat(active.getStatus()).isEqualTo(UserSession.Status.ROTATED);
    assertThat(rotated.session().getFamilyId()).isEqualTo("fam-1");
  }

  @Test
  void rotateDetectsReuseAndRevokesFamily() {
    UserSession reused = activeSession("tok-1", "fam-1");
    reused.rotate(clock.instant()); // already rotated
    when(repo.findByRefreshTokenHash(SessionService.hash("tok-1"))).thenReturn(Optional.of(reused));
    when(repo.findByFamilyId("fam-1")).thenReturn(List.of(reused));

    assertThatThrownBy(() -> service.rotate("tok-1", "ip", "agent"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("no longer valid");
    verify(repo, atLeastOnce()).save(any(UserSession.class));
  }

  @Test
  void rotateRejectsUnknownToken() {
    when(repo.findByRefreshTokenHash(any())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.rotate("nope", "ip", "agent"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("Invalid refresh token");
  }

  private UserSession activeSession(String token, String family) {
    return new UserSession(
        "sess-" + token,
        "user1",
        "device1",
        SessionService.hash(token),
        family,
        "1.2.3.4",
        "agent",
        clock.instant(),
        clock.instant().plusSeconds(86400));
  }
}
