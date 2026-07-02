package com.paywithease.payout.application;

/**
 * Abstraction over a disbursement provider (RazorpayX, Cashfree Payouts, etc.). Implementations are
 * ordered; the {@link PayoutGatewayRouter} tries them in turn to achieve failover. Real
 * integrations are asynchronous (confirmed by a payout webhook) — Sprint 6 models a synchronous
 * disburse with failover; the async confirmation is a later refinement.
 */
public interface PayoutGateway {

  String name();

  /** Whether this gateway is currently usable (health/circuit state). */
  boolean isAvailable();

  DisburseResult disburse(String accountReference, long amountMinor, String reference);

  record DisburseResult(boolean success, String providerRef, String failureReason) {
    public static DisburseResult ok(String providerRef) {
      return new DisburseResult(true, providerRef, null);
    }

    public static DisburseResult fail(String reason) {
      return new DisburseResult(false, null, reason);
    }
  }
}
