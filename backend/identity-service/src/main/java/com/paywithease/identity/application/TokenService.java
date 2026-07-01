package com.paywithease.identity.application;

import com.paywithease.identity.config.JwtConfig;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues short-lived RS256 access tokens carrying the tenant, roles, and permission scope. */
@Service
public class TokenService {

  private final JwtEncoder jwtEncoder;
  private final Clock clock;
  private final String issuer;
  private final Duration accessTokenTtl;

  public TokenService(
      JwtEncoder jwtEncoder,
      Clock clock,
      @Value("${peb.security.jwt.issuer:https://identity.paywithease.local}") String issuer,
      @Value("${peb.security.jwt.access-ttl-seconds:900}") long accessTtlSeconds) {
    this.jwtEncoder = jwtEncoder;
    this.clock = clock;
    this.issuer = issuer;
    this.accessTokenTtl = Duration.ofSeconds(accessTtlSeconds);
  }

  public IssuedAccessToken issue(
      String userId, String tenantId, List<String> roles, List<String> permissions) {
    Instant now = clock.instant();
    Instant exp = now.plus(accessTokenTtl);
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(now)
            .expiresAt(exp)
            .subject(userId)
            .claim("tenant_id", tenantId == null ? "" : tenantId)
            .claim("roles", roles)
            .claim("scope", String.join(" ", permissions))
            .claim("typ", "access")
            .build();
    JwsHeader header = JwsHeader.with(() -> "RS256").keyId(JwtConfig.KEY_ID).build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new IssuedAccessToken(token, accessTokenTtl.getSeconds());
  }

  public record IssuedAccessToken(String token, long expiresInSeconds) {}
}
