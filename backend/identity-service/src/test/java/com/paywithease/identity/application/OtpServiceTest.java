package com.paywithease.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.security.BlindIndex;
import com.paywithease.identity.domain.OtpAudit;
import com.paywithease.identity.infrastructure.OtpAuditRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

  @Mock StringRedisTemplate redis;
  @Mock ValueOperations<String, String> valueOps;
  @Mock OtpAuditRepository auditRepository;

  private OtpService otp;
  private final BlindIndex blindIndex = new BlindIndex(new byte[32]);
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    otp = new OtpService(redis, blindIndex, auditRepository, clock, "pepper", 300, 5, 3600, false);
    when(redis.opsForValue()).thenReturn(valueOps);
  }

  @Test
  void requestStoresOtpAndAudits() {
    when(valueOps.increment(anyString())).thenReturn(1L); // rate-limit counter
    long ttl = otp.request("9876543210", "LOGIN").ttl();
    assertThat(ttl).isEqualTo(300);
    verify(valueOps).set(anyString(), anyString(), any(java.time.Duration.class));
    verify(auditRepository).save(any(OtpAudit.class));
  }

  @Test
  void requestRateLimited() {
    when(valueOps.increment(anyString())).thenReturn(6L); // over the limit of 5
    assertThatThrownBy(() -> otp.request("9876543210", "LOGIN"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("Too many OTP requests");
  }

  @Test
  void verifySucceedsForCorrectCode() {
    String mobileHash = blindIndex.hash("9876543210");
    String salt = "abcd1234";
    String stored = salt + "$" + otp.hash(salt, "123456");
    when(valueOps.get(eq("otp:LOGIN:" + mobileHash))).thenReturn(stored);
    when(valueOps.increment(eq("otp:att:LOGIN:" + mobileHash))).thenReturn(1L);

    assertThat(otp.verify("9876543210", "LOGIN", "123456")).isEqualTo(mobileHash);
    verify(redis).delete(eq("otp:LOGIN:" + mobileHash));
  }

  @Test
  void verifyFailsForWrongCode() {
    String salt = "abcd1234";
    when(valueOps.get(anyString())).thenReturn(salt + "$" + otp.hash(salt, "123456"));
    when(valueOps.increment(anyString())).thenReturn(1L);
    assertThatThrownBy(() -> otp.verify("9876543210", "LOGIN", "000000"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("Incorrect OTP");
  }

  @Test
  void verifyFailsWhenExpired() {
    when(valueOps.get(anyString())).thenReturn(null);
    assertThatThrownBy(() -> otp.verify("9876543210", "LOGIN", "123456"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("expired");
  }

  @Test
  void verifyLocksOutAfterTooManyAttempts() {
    when(valueOps.get(anyString())).thenReturn("salt$hash");
    when(valueOps.increment(anyString())).thenReturn(6L);
    assertThatThrownBy(() -> otp.verify("9876543210", "LOGIN", "123456"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("Too many attempts");
  }
}
