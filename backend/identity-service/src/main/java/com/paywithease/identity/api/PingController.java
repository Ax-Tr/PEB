package com.paywithease.identity.api;

import com.paywithease.common.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Clock;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Liveness/ping endpoint used by the Sprint 0 smoke test through the gateway. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "meta", description = "Service metadata & health")
public class PingController {

  private final Clock clock;

  public PingController(Clock clock) {
    this.clock = clock;
  }

  @GetMapping("/ping")
  @Operation(summary = "Ping the identity service")
  public Map<String, Object> ping() {
    return Map.of(
        "service",
        "identity-service",
        "status",
        "ok",
        "time",
        clock.instant().toString(),
        "tenant",
        TenantContext.current().map(TenantContext.Principal::tenantId).orElse("none"));
  }
}
