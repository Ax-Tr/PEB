package com.paywithease.common.config;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared beans provided to every service that scans {@code com.paywithease.common}. A single UTC
 * {@link Clock} is injected everywhere (never {@code Instant.now()} directly) so time is testable.
 */
@Configuration
public class CommonBeansConfig {

  @Bean
  @ConditionalOnMissingBean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
