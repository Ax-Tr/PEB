package com.paywithease.payout.application;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Tries the configured payout gateways in order until one succeeds — provider failover. Returns the
 * provider that ultimately disbursed (or a failure if all are exhausted). Each attempt is counted
 * so the caller can record {@code gateway_attempts}.
 */
@Component
public class PayoutGatewayRouter {

  private static final Logger log = LoggerFactory.getLogger(PayoutGatewayRouter.class);

  private final List<PayoutGateway> gateways;

  public PayoutGatewayRouter(List<PayoutGateway> gateways) {
    this.gateways = gateways;
  }

  public record RoutedResult(boolean success, String provider, String providerRef, int attempts) {}

  public RoutedResult disburse(String accountReference, long amountMinor, String reference) {
    int attempts = 0;
    for (PayoutGateway gateway : gateways) {
      if (!gateway.isAvailable()) {
        continue;
      }
      attempts++;
      try {
        PayoutGateway.DisburseResult result =
            gateway.disburse(accountReference, amountMinor, reference);
        if (result.success()) {
          return new RoutedResult(true, gateway.name(), result.providerRef(), attempts);
        }
        log.warn(
            "Gateway {} declined payout {}: {}", gateway.name(), reference, result.failureReason());
      } catch (RuntimeException e) {
        log.warn("Gateway {} failed for payout {}: {}", gateway.name(), reference, e.getMessage());
      }
    }
    return new RoutedResult(false, null, null, attempts);
  }
}
