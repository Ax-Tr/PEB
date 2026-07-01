package com.paywithease.identity.application;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.ids.Ulid;
import com.paywithease.identity.domain.UserSession;
import com.paywithease.identity.infrastructure.UserSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages refresh-token sessions with rotation and theft detection. The opaque refresh token is
 * returned to the client once and only its SHA-256 hash is stored. Reuse of a rotated token revokes
 * the entire token family (a strong signal of token theft).
 */
@Service
public class SessionService {

  private static final Logger log = LoggerFactory.getLogger(SessionService.class);

  private final UserSessionRepository sessions;
  private final Clock clock;
  private final Duration refreshTtl;
  private final SecureRandom random = new SecureRandom();

  public SessionService(
      UserSessionRepository sessions,
      Clock clock,
      @Value("${peb.security.jwt.refresh-ttl-days:30}") long refreshTtlDays) {
    this.sessions = sessions;
    this.clock = clock;
    this.refreshTtl = Duration.ofDays(refreshTtlDays);
  }

  public record Issued(UserSession session, String refreshToken) {}

  @Transactional
  public Issued create(String userId, String deviceId, String ip, String userAgent) {
    String familyId = Ulid.newId();
    return persist(userId, deviceId, ip, userAgent, familyId);
  }

  @Transactional
  public Issued rotate(String refreshToken, String ip, String userAgent) {
    String hash = hash(refreshToken);
    UserSession current =
        sessions
            .findByRefreshTokenHash(hash)
            .orElseThrow(
                () -> new ApiException(ErrorCode.UNAUTHENTICATED, "Invalid refresh token"));

    Instant now = clock.instant();
    if (current.getStatus() != UserSession.Status.ACTIVE) {
      // Reuse of a non-active token => probable theft: revoke the whole family.
      revokeFamily(current.getFamilyId(), now);
      log.warn("Refresh token reuse detected; revoked family {}", current.getFamilyId());
      throw new ApiException(ErrorCode.UNAUTHENTICATED, "Refresh token no longer valid");
    }
    if (!current.isActive(now)) {
      current.revoke(now);
      throw new ApiException(ErrorCode.UNAUTHENTICATED, "Session expired");
    }

    current.rotate(now);
    sessions.save(current);
    return persist(
        current.getUserId(), current.getDeviceId(), ip, userAgent, current.getFamilyId());
  }

  @Transactional
  public void revoke(String sessionId, String userId) {
    UserSession session =
        sessions.findById(sessionId).orElseThrow(() -> ApiException.notFound("Session"));
    if (!session.getUserId().equals(userId)) {
      throw ApiException.forbidden();
    }
    session.revoke(clock.instant());
    sessions.save(session);
  }

  @Transactional(readOnly = true)
  public List<UserSession> listActive(String userId) {
    return sessions.findByUserIdAndStatus(userId, UserSession.Status.ACTIVE);
  }

  private void revokeFamily(String familyId, Instant now) {
    for (UserSession s : sessions.findByFamilyId(familyId)) {
      if (s.getStatus() == UserSession.Status.ACTIVE
          || s.getStatus() == UserSession.Status.ROTATED) {
        s.revoke(now);
        sessions.save(s);
      }
    }
  }

  private Issued persist(
      String userId, String deviceId, String ip, String userAgent, String familyId) {
    Instant now = clock.instant();
    String refreshToken = newOpaqueToken();
    UserSession session =
        new UserSession(
            Ulid.newId(),
            userId,
            deviceId,
            hash(refreshToken),
            familyId,
            ip,
            userAgent,
            now,
            now.plus(refreshTtl));
    sessions.save(session);
    return new Issued(session, refreshToken);
  }

  private String newOpaqueToken() {
    byte[] b = new byte[32];
    random.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  static String hash(String token) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Refresh token hashing failed", e);
    }
  }
}
