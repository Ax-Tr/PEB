package com.paywithease.payout.config;

import com.paywithease.payout.application.PayoutGateway;
import com.paywithease.payout.application.StubPayoutGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Registers ordered payout gateways for failover. Sprint 6 wires two stub providers (primary +
 * secondary); replace with real provider adapters per environment. Order determines failover
 * priority.
 */
@Configuration
public class PayoutGatewayConfig {

  @Bean
  @Order(1)
  public PayoutGateway primaryGateway() {
    return new StubPayoutGateway("razorpayx", true, true);
  }

  @Bean
  @Order(2)
  public PayoutGateway secondaryGateway() {
    return new StubPayoutGateway("cashfree", true, true);
  }
}
