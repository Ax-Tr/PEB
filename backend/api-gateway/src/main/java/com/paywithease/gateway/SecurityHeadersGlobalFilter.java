package com.paywithease.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Adds hardening response headers to every response at the edge (OWASP Secure Headers). Applied
 * once at the gateway so all services are covered uniformly. The API is JSON-only and never
 * rendered as a page, so the CSP is a strict lock-down and framing is denied.
 */
@Component
public class SecurityHeadersGlobalFilter implements GlobalFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    // Set headers before the response commits.
    exchange
        .getResponse()
        .beforeCommit(
            () -> {
              HttpHeaders h = exchange.getResponse().getHeaders();
              setIfAbsent(h, "X-Content-Type-Options", "nosniff");
              setIfAbsent(h, "X-Frame-Options", "DENY");
              setIfAbsent(h, "Referrer-Policy", "no-referrer");
              setIfAbsent(
                  h, "Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
              setIfAbsent(
                  h, "Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
              setIfAbsent(h, "Permissions-Policy", "geolocation=(), camera=(), microphone=()");
              setIfAbsent(h, "Cache-Control", "no-store");
              // Do not advertise the server implementation.
              h.remove("Server");
              h.remove("X-Powered-By");
              return Mono.empty();
            });
    return chain.filter(exchange);
  }

  private static void setIfAbsent(HttpHeaders headers, String name, String value) {
    if (!headers.containsKey(name)) {
      headers.set(name, value);
    }
  }

  @Override
  public int getOrder() {
    // After correlation, but it only mutates the response so ordering is not critical.
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }
}
