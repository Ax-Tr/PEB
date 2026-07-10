package com.paywithease.identity.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.error.ApiException;
import com.paywithease.common.error.ErrorCode;
import com.paywithease.common.event.EventEnvelope;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.outbox.OutboxWriter;
import com.paywithease.common.security.BlindIndex;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.identity.domain.DataConsentRecord;
import com.paywithease.identity.domain.Device;
import com.paywithease.identity.domain.MobileNumber;
import com.paywithease.identity.domain.Role;
import com.paywithease.identity.domain.User;
import com.paywithease.identity.domain.UserRole;
import com.paywithease.identity.domain.UserSession;
import com.paywithease.identity.infrastructure.ConsentRepository;
import com.paywithease.identity.infrastructure.DeviceRepository;
import com.paywithease.identity.infrastructure.RoleRepository;
import com.paywithease.identity.infrastructure.UserRepository;
import com.paywithease.identity.infrastructure.UserRoleRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates the OTP login flow, self-onboarding, device binding, consent, and token issuance.
 */
@Service
public class AuthService {

  private static final String PURPOSE_LOGIN = "LOGIN";
  private static final String DEFAULT_ROLE = "OWNER";

  private final OtpService otpService;
  private final TokenService tokenService;
  private final SessionService sessionService;
  private final UserRepository users;
  private final RoleRepository roles;
  private final UserRoleRepository userRoles;
  private final DeviceRepository devices;
  private final ConsentRepository consents;
  private final BlindIndex blindIndex;
  private final AuditWriter audit;
  private final OutboxWriter outbox;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public AuthService(
      OtpService otpService,
      TokenService tokenService,
      SessionService sessionService,
      UserRepository users,
      RoleRepository roles,
      UserRoleRepository userRoles,
      DeviceRepository devices,
      ConsentRepository consents,
      BlindIndex blindIndex,
      AuditWriter audit,
      OutboxWriter outbox,
      ObjectMapper objectMapper,
      Clock clock) {
    this.otpService = otpService;
    this.tokenService = tokenService;
    this.sessionService = sessionService;
    this.users = users;
    this.roles = roles;
    this.userRoles = userRoles;
    this.devices = devices;
    this.consents = consents;
    this.blindIndex = blindIndex;
    this.audit = audit;
    this.outbox = outbox;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Transactional
  public OtpService.RequestResult requestOtp(String rawMobile) {
    var result = otpService.request(rawMobile, PURPOSE_LOGIN);
    audit.record("OTP_REQUESTED", "user", null, Map.of("purpose", PURPOSE_LOGIN));
    return result;
  }

  public boolean isLogOtpForDev() {
    return otpService.isLogOtpForDev();
  }

  public record DeviceInfo(String fingerprint, String platform, String model) {}

  public record AuthResult(
      String userId,
      String tenantId,
      List<String> roles,
      String accessToken,
      long accessExpiresIn,
      String refreshToken,
      String sessionId,
      boolean newUser) {}

  @Transactional
  public AuthResult verifyOtpAndLogin(
      String rawMobile,
      String code,
      DeviceInfo deviceInfo,
      String ip,
      String userAgent,
      String consentNoticeVersion) {
    String mobileHash = otpService.verify(rawMobile, PURPOSE_LOGIN, code);
    String normalizedMobile = MobileNumber.of(rawMobile).value();

    boolean newUser = false;
    User user = users.findByMobileHash(mobileHash).orElse(null);
    if (user == null) {
      user = new User(Ulid.newId(), normalizedMobile, mobileHash, clock.instant());
      users.saveAndFlush(user);
      grantDefaultRole(user);
      recordConsent(user.getId(), consentNoticeVersion);
      newUser = true;
    }

    // Now that the acting user is known, enrich the context so audit/outbox capture the actor.
    TenantContext.set(
        new TenantContext.Principal(
            user.getTenantId(),
            user.getTenantId(),
            user.getId(),
            TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null)));

    Device device = bindDevice(user.getId(), deviceInfo);
    List<String> roleNames = userRoles.findRoleNames(user.getId());
    List<String> permissions = userRoles.findPermissionCodes(user.getId());

    var access = tokenService.issue(user.getId(), user.getTenantId(), roleNames, permissions);
    var session =
        sessionService.create(user.getId(), device == null ? null : device.getId(), ip, userAgent);

    if (newUser) {
      emit("USER_CREATED", user);
    }
    emit("USER_LOGGED_IN", user);
    audit.record(
        "LOGIN_SUCCEEDED",
        "user",
        user.getId(),
        Map.of("newUser", newUser, "sessionId", session.session().getId()));

    return new AuthResult(
        user.getId(),
        user.getTenantId(),
        roleNames,
        access.token(),
        access.expiresInSeconds(),
        session.refreshToken(),
        session.session().getId(),
        newUser);
  }

  @Transactional
  public AuthResult refresh(String refreshToken, String ip, String userAgent) {
    SessionService.Issued rotated = sessionService.rotate(refreshToken, ip, userAgent);
    UserSession session = rotated.session();
    User user =
        users.findById(session.getUserId()).orElseThrow(() -> ApiException.notFound("User"));
    List<String> roleNames = userRoles.findRoleNames(user.getId());
    List<String> permissions = userRoles.findPermissionCodes(user.getId());
    var access = tokenService.issue(user.getId(), user.getTenantId(), roleNames, permissions);
    return new AuthResult(
        user.getId(),
        user.getTenantId(),
        roleNames,
        access.token(),
        access.expiresInSeconds(),
        rotated.refreshToken(),
        session.getId(),
        false);
  }

  @Transactional
  public void logout(String sessionId, String userId) {
    sessionService.revoke(sessionId, userId);
    audit.record("SESSION_REVOKED", "user_session", sessionId, Map.of());
  }

  /**
   * Links a newly-created business (tenantId) to the user record in the identity DB. Must only be
   * called by the user themselves (validated by the caller via JWT subject). The next token refresh
   * will embed the tenantId in the JWT and unlock all tenant-scoped APIs.
   */
  @Transactional
  public void linkTenant(String userId, String tenantId) {
    User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
    if (user.getTenantId() != null && !user.getTenantId().isBlank()) {
      // Already linked — idempotent if same tenant, conflict otherwise
      if (!user.getTenantId().equals(tenantId)) {
        throw new ApiException(ErrorCode.CONFLICT, "User already linked to a different tenant");
      }
      return;
    }
    user.attachTenant(tenantId, clock.instant());
    audit.record("TENANT_LINKED", "user", userId, Map.of("tenantId", tenantId));
  }

  @Transactional(readOnly = true)
  public List<UserSession> sessions(String userId) {
    return sessionService.listActive(userId);
  }

  private void grantDefaultRole(User user) {
    Role owner =
        roles
            .findByName(DEFAULT_ROLE)
            .orElseThrow(() -> new ApiException(ErrorCode.INTERNAL_ERROR, "OWNER role not seeded"));
    userRoles.saveAndFlush(
        new UserRole(
            user.getId(), owner.getId(), user.getTenantId(), user.getId(), clock.instant()));
  }

  private void recordConsent(String userId, String noticeVersion) {
    String version = noticeVersion == null || noticeVersion.isBlank() ? "v1" : noticeVersion;
    consents.save(
        new DataConsentRecord(
            Ulid.newId(), userId, "ACCOUNT_CREATION", version, true, clock.instant()));
    audit.record(
        "CONSENT_RECORDED",
        "user",
        userId,
        Map.of("purpose", "ACCOUNT_CREATION", "version", version));
  }

  private Device bindDevice(String userId, DeviceInfo info) {
    if (info == null || info.fingerprint() == null || info.fingerprint().isBlank()) {
      return null;
    }
    String deviceHash = blindIndex.hash(info.fingerprint());
    Device device =
        devices
            .findByUserIdAndDeviceHash(userId, deviceHash)
            .map(
                d -> {
                  d.touch(clock.instant());
                  return d;
                })
            .orElseGet(
                () ->
                    new Device(
                        Ulid.newId(),
                        userId,
                        deviceHash,
                        info.platform(),
                        info.model(),
                        clock.instant()));
    return devices.save(device);
  }

  private void emit(String eventType, User user) {
    var payload =
        objectMapper.createObjectNode().put("userId", user.getId()).put("status", user.getStatus());
    EventEnvelope envelope =
        EventEnvelope.builder()
            .eventType(eventType)
            .tenantId(user.getTenantId())
            .businessId(user.getTenantId())
            .sourceService("identity-service")
            .actorId(user.getId())
            .aggregateId(user.getId())
            .correlationId(
                TenantContext.current().map(TenantContext.Principal::correlationId).orElse(null))
            .payload(payload)
            .build(clock.instant());
    outbox.append(envelope);
  }
}
