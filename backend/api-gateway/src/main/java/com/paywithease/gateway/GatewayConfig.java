package com.paywithease.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Gateway beans. Sprint 0 rate-limits by client IP; Sprint 1 switches the key to the authenticated
 * principal/tenant once JWT resource-server validation is in place.
 */
@Configuration
public class GatewayConfig {

  @Bean
  public KeyResolver ipKeyResolver() {
    return exchange ->
        Mono.just(
            exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown");
  }
}
