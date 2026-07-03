package com.paywithease.notification.api;

import com.paywithease.notification.application.DeviceRegistrationService;
import com.paywithease.notification.domain.DeviceToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Register/unregister a device's push token for the current tenant/user. */
@RestController
@RequestMapping("/api/v1/notifications/devices")
@Tag(name = "devices", description = "Push device-token registration")
public class DeviceController {

  private final DeviceRegistrationService service;

  public DeviceController(DeviceRegistrationService service) {
    this.service = service;
  }

  @PostMapping
  @Operation(summary = "Register (or refresh) this device's push token; idempotent per token")
  public NotificationDtos.DeviceResponse register(
      @Valid @RequestBody NotificationDtos.RegisterDeviceRequest body) {
    DeviceToken d = service.register(body.token(), body.platform());
    return new NotificationDtos.DeviceResponse(d.getId(), d.getPlatform(), d.isActive());
  }

  @DeleteMapping("/{token}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Unregister a device token (e.g. on logout)")
  public void unregister(@PathVariable String token) {
    service.unregister(token);
  }

  @GetMapping
  @Operation(summary = "List the tenant's active device tokens")
  public List<NotificationDtos.DeviceResponse> list() {
    return service.activeDevices().stream()
        .map(d -> new NotificationDtos.DeviceResponse(d.getId(), d.getPlatform(), d.isActive()))
        .toList();
  }
}
