package com.paywithease.payout.application;

import com.paywithease.common.ids.Ulid;

/**
 * Development/no-op payout gateway. Returns a synthetic provider reference. Real provider adapters
 * (RazorpayX, Cashfree) replace these in each environment; the {@link PayoutGatewayRouter} failover
 * behaviour is unchanged. {@code available}/{@code succeed} are configurable so failover can be
 * exercised in tests and staging.
 */
public class StubPayoutGateway implements PayoutGateway {

  private final String name;
  private final boolean available;
  private final boolean succeed;

  public StubPayoutGateway(String name, boolean available, boolean succeed) {
    this.name = name;
    this.available = available;
    this.succeed = succeed;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public boolean isAvailable() {
    return available;
  }

  @Override
  public DisburseResult disburse(String accountReference, long amountMinor, String reference) {
    return succeed
        ? DisburseResult.ok(name + "_" + Ulid.newId())
        : DisburseResult.fail(name + " unavailable");
  }
}
