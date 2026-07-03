package com.paywithease.notification.application;

import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.ids.Ulid;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.notification.domain.DeviceToken;
import com.paywithease.notification.infrastructure.DeviceTokenRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers and revokes push device tokens for the current tenant/user. Registration is idempotent
 * per (tenant, token): a re-registered token is reactivated and touched rather than duplicated, so
 * the same device calling on every launch never creates rows. Tokens are soft-revoked on logout.
 */
@Service
public class DeviceRegistrationService {

  private final DeviceTokenRepository devices;
  private final AuditWriter audit;
  private final Clock clock;

  public DeviceRegistrationService(DeviceTokenRepository devices, AuditWriter audit, Clock clock) {
    this.devices = devices;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional
  public DeviceToken register(String token, String platform) {
    String tenantId = TenantContext.requireTenantId();
    String userId = TenantContext.actorId().orElse(null);
    DeviceToken device =
        devices
            .findByTenantIdAndToken(tenantId, token)
            .map(
                existing -> {
                  existing.touch(userId, platform, clock.instant());
                  return existing;
                })
            .orElseGet(
                () ->
                    new DeviceToken(
                        Ulid.newId(), tenantId, userId, token, platform, clock.instant()));
    devices.save(device);
    audit.record(
        "DEVICE_TOKEN_REGISTERED", "device_token", device.getId(), Map.of("platform", platform));
    return device;
  }

  @Transactional
  public void unregister(String token) {
    String tenantId = TenantContext.requireTenantId();
    devices
        .findByTenantIdAndToken(tenantId, token)
        .ifPresent(
            device -> {
              device.revoke(clock.instant());
              devices.save(device);
              audit.record("DEVICE_TOKEN_REVOKED", "device_token", device.getId(), Map.of());
            });
  }

  @Transactional(readOnly = true)
  public List<DeviceToken> activeDevices() {
    return devices.findByTenantIdAndActiveTrueOrderByUpdatedAtDesc(TenantContext.requireTenantId());
  }
}
