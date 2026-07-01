package com.paywithease.common.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables scheduling + a default {@link TopicResolver} only when the relay is switched on. */
@Configuration
@ConditionalOnProperty(name = "peb.outbox.relay.enabled", havingValue = "true")
@EnableScheduling
public class OutboxRelayConfig {

  @Bean
  @ConditionalOnMissingBean
  public TopicResolver topicResolver() {
    return TopicResolver.defaultResolver();
  }
}
