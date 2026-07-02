package com.paywithease.payout.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PayoutGatewayRouterTest {

  @Test
  void usesPrimaryWhenAvailable() {
    var router =
        new PayoutGatewayRouter(
            List.of(
                new StubPayoutGateway("primary", true, true),
                new StubPayoutGateway("secondary", true, true)));
    var r = router.disburse("acc", 1000, "ref");
    assertThat(r.success()).isTrue();
    assertThat(r.provider()).isEqualTo("primary");
    assertThat(r.attempts()).isEqualTo(1);
  }

  @Test
  void failsOverToSecondaryWhenPrimaryUnavailable() {
    var router =
        new PayoutGatewayRouter(
            List.of(
                new StubPayoutGateway("primary", false, true),
                new StubPayoutGateway("secondary", true, true)));
    var r = router.disburse("acc", 1000, "ref");
    assertThat(r.success()).isTrue();
    assertThat(r.provider()).isEqualTo("secondary");
  }

  @Test
  void failsOverWhenPrimaryDeclines() {
    var router =
        new PayoutGatewayRouter(
            List.of(
                new StubPayoutGateway("primary", true, false),
                new StubPayoutGateway("secondary", true, true)));
    var r = router.disburse("acc", 1000, "ref");
    assertThat(r.success()).isTrue();
    assertThat(r.provider()).isEqualTo("secondary");
    assertThat(r.attempts()).isEqualTo(2);
  }

  @Test
  void failsWhenAllGatewaysExhausted() {
    var router =
        new PayoutGatewayRouter(
            List.of(
                new StubPayoutGateway("primary", true, false),
                new StubPayoutGateway("secondary", true, false)));
    var r = router.disburse("acc", 1000, "ref");
    assertThat(r.success()).isFalse();
    assertThat(r.provider()).isNull();
  }
}
