package com.paywithease.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.paywithease.common.audit.AuditWriter;
import com.paywithease.common.tenant.TenantContext;
import com.paywithease.notification.domain.DeviceToken;
import com.paywithease.notification.infrastructure.DeviceTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceRegistrationServiceTest {

  @Mock DeviceTokenRepository devices;
  @Mock AuditWriter audit;

  private DeviceRegistrationService service;
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service = new DeviceRegistrationService(devices, audit, clock);
    TenantContext.set(new TenantContext.Principal("tenant1", "tenant1", "user1", "corr1"));
    when(devices.save(any())).thenAnswer(returnsFirstArg());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void registersANewToken() {
    when(devices.findByTenantIdAndToken("tenant1", "tok-1")).thenReturn(Optional.empty());
    DeviceToken d = service.register("tok-1", "android");
    assertThat(d.isActive()).isTrue();
    assertThat(d.getPlatform()).isEqualTo("android");
    verify(devices).save(any(DeviceToken.class));
  }

  @Test
  void reRegisteringExistingTokenTouchesItIdempotently() {
    DeviceToken existing =
        new DeviceToken("d1", "tenant1", "user1", "tok-1", "android", clock.instant());
    existing.revoke(clock.instant()); // was revoked; re-register should reactivate
    when(devices.findByTenantIdAndToken("tenant1", "tok-1")).thenReturn(Optional.of(existing));

    DeviceToken d = service.register("tok-1", "ios");

    assertThat(d.getId()).isEqualTo("d1"); // same row, not a duplicate
    assertThat(d.isActive()).isTrue();
    assertThat(d.getPlatform()).isEqualTo("ios");
  }

  @Test
  void unregisterRevokesToken() {
    DeviceToken existing =
        new DeviceToken("d1", "tenant1", "user1", "tok-1", "android", clock.instant());
    when(devices.findByTenantIdAndToken("tenant1", "tok-1")).thenReturn(Optional.of(existing));
    service.unregister("tok-1");
    assertThat(existing.isActive()).isFalse();
    verify(devices).save(existing);
  }

  @Test
  void unregisterUnknownTokenIsANoOp() {
    when(devices.findByTenantIdAndToken("tenant1", "nope")).thenReturn(Optional.empty());
    service.unregister("nope");
    verify(devices, never()).save(any());
  }
}
