package com.paywithease.gateway;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Circuit-breaker fallback responses (RFC-7807 shaped) when a downstream service is unavailable.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

  @GetMapping("/identity")
  @PostMapping("/identity")
  public ResponseEntity<Map<String, Object>> identityFallback() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            Map.of(
                "type", "https://errors.paywithease.com/SERVICE_UNAVAILABLE",
                "title", "SERVICE_UNAVAILABLE",
                "status", 503,
                "detail", "identity-service is temporarily unavailable, please retry",
                "code", "SERVICE_UNAVAILABLE"));
  }
}
