package com.paywithease.identity.application;

import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.security.BlindIndex;
import com.paywithease.identity.domain.MobileNumber;
import com.paywithease.identity.domain.OtpAudit;
import com.paywithease.identity.infrastructure.OtpAuditRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies one-time passwords. Live OTP state is in Redis (hashed, short TTL,
 * single-use, attempt-limited); a per-mobile sliding window rate-limits issuance. The plaintext OTP
 * is never persisted or logged in production (it is delivered asynchronously via the notification
 * service — stubbed to a debug log in local/dev).
 */
@Service
public class OtpService {

  private static final Logger log = LoggerFactory.getLogger(OtpService.class);
  private static final int OTP_DIGITS = 6;
  private static final int MAX_ATTEMPTS = 5;

  private final StringRedisTemplate redis;
  private final BlindIndex blindIndex;
  private final OtpAuditRepository auditRepository;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  private final String pepper;
  private final Duration ttl;
  private final int maxPerWindow;
  private final Duration rateWindow;
  private final boolean logOtpForDev;

  public OtpService(
      StringRedisTemplate redis,
      BlindIndex blindIndex,
      OtpAuditRepository auditRepository,
      Clock clock,
      @Value("${peb.otp.pepper:dev-otp-pepper}") String pepper,
      @Value("${peb.otp.ttl-seconds:300}") long ttlSeconds,
      @Value("${peb.otp.max-per-window:5}") int maxPerWindow,
      @Value("${peb.otp.window-seconds:3600}") long windowSeconds,
      @Value("${peb.otp.log-for-dev:true}") boolean logOtpForDev) {
    this.redis = redis;
    this.blindIndex = blindIndex;
    this.auditRepository = auditRepository;
    this.clock = clock;
    this.pepper = pepper;
    this.ttl = Duration.ofSeconds(ttlSeconds);
    this.maxPerWindow = maxPerWindow;
    this.rateWindow = Duration.ofSeconds(windowSeconds);
    this.logOtpForDev = logOtpForDev;
  }

  public record RequestResult(long ttl, String otp) {}

  public boolean isLogOtpForDev() {
    return logOtpForDev;
  }

  /** Issues an OTP for a mobile+purpose. Returns the request result. Throws if rate-limited. */
  public RequestResult request(String rawMobile, String purpose) {
    String mobileHash = blindIndex.hash(MobileNumber.of(rawMobile).value());
    enforceRateLimit(mobileHash);

    String otp = generateOtp();
    String salt = randomSalt();
    redis.opsForValue().set(otpKey(purpose, mobileHash), salt + "$" + hash(salt, otp), ttl);
    redis.delete(attemptsKey(purpose, mobileHash));

    auditRepository.save(
        new OtpAudit(Ulid.newId(), mobileHash, purpose, "ISSUED", clock.instant()));
    if (logOtpForDev) {
      log.debug("DEV OTP for purpose={} = {} (not logged in production)", purpose, otp);
    }
    return new RequestResult(ttl.getSeconds(), otp);
  }

  /**
   * Verifies an OTP. Consumes it on success. Returns the mobile blind-index hash for the caller.
   */
  public String verify(String rawMobile, String purpose, String code) {
    String mobileHash = blindIndex.hash(MobileNumber.of(rawMobile).value());
    String stored = redis.opsForValue().get(otpKey(purpose, mobileHash));
    if (stored == null) {
      audit(mobileHash, purpose, "EXPIRED");
      throw new ApiException(ErrorCode.UNAUTHENTICATED, "OTP expired or not requested");
    }

    Long attempts = redis.opsForValue().increment(attemptsKey(purpose, mobileHash));
    if (attempts != null && attempts > MAX_ATTEMPTS) {
      redis.delete(otpKey(purpose, mobileHash));
      audit(mobileHash, purpose, "FAILED");
      throw new ApiException(ErrorCode.UNAUTHENTICATED, "Too many attempts; request a new OTP");
    }

    int sep = stored.indexOf('$');
    String salt = stored.substring(0, sep);
    String expected = stored.substring(sep + 1);
    if (!constantTimeEquals(expected, hash(salt, code))) {
      audit(mobileHash, purpose, "FAILED");
      throw new ApiException(ErrorCode.UNAUTHENTICATED, "Incorrect OTP");
    }

    redis.delete(otpKey(purpose, mobileHash));
    redis.delete(attemptsKey(purpose, mobileHash));
    audit(mobileHash, purpose, "VERIFIED");
    return mobileHash;
  }

  private void enforceRateLimit(String mobileHash) {
    String key = "otp:rl:" + mobileHash;
    Long count = redis.opsForValue().increment(key);
    if (count != null && count == 1L) {
      redis.expire(key, rateWindow);
    }
    if (count != null && count > maxPerWindow) {
      throw new ApiException(ErrorCode.CONFLICT, "Too many OTP requests; try again later");
    }
  }

  private void audit(String mobileHash, String purpose, String status) {
    auditRepository.save(new OtpAudit(Ulid.newId(), mobileHash, purpose, status, clock.instant()));
  }

  private String generateOtp() {
    int bound = (int) Math.pow(10, OTP_DIGITS);
    return String.format("%0" + OTP_DIGITS + "d", random.nextInt(bound));
  }

  private String randomSalt() {
    byte[] b = new byte[8];
    random.nextBytes(b);
    return HexFormat.of().formatHex(b);
  }

  String hash(String salt, String code) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest((pepper + salt + code).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (Exception e) {
      throw new IllegalStateException("OTP hashing failed", e);
    }
  }

  private static boolean constantTimeEquals(String a, String b) {
    return MessageDigest.isEqual(
        a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
  }

  private static String otpKey(String purpose, String mobileHash) {
    return "otp:" + purpose + ":" + mobileHash;
  }

  private static String attemptsKey(String purpose, String mobileHash) {
    return "otp:att:" + purpose + ":" + mobileHash;
  }
}
