package com.paywithease.business.api;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Health/ping endpoint for business-service. */
@RestController
@RequestMapping("/api/v1")
public class PingController {

  @GetMapping("/ping")
  public Map<String, Object> ping() {
    return Map.of(
        "status", "ok",
        "service", "business-service",
        "timestamp", Instant.now().toString());
  }
}
