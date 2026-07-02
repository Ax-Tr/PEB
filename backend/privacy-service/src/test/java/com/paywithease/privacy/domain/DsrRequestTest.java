package com.paywithease.privacy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paywithease.common.error.ApiException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DsrRequestTest {

  private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");
  private static final Instant DUE = NOW.plusSeconds(30L * 24 * 3600);

  private DsrRequest request(DsrType type) {
    return new DsrRequest("r1", "t1", type, "subj1", "user@example.com", "please erase", NOW, DUE);
  }

  @Test
  void happyPathReceivedToCompleted() {
    DsrRequest r = request(DsrType.ERASURE);
    r.startVerification();
    r.markVerified("dpo1", NOW);
    r.attachErasurePlan("[]");
    r.complete("s3://evidence.zip", "3 retained under legal hold", NOW);
    assertThat(r.getStatus()).isEqualTo("COMPLETED");
    assertThat(r.getEvidenceRef()).isEqualTo("s3://evidence.zip");
  }

  @Test
  void cannotCompleteWithoutVerification() {
    DsrRequest r = request(DsrType.ACCESS);
    // still RECEIVED
    assertThatThrownBy(() -> r.complete("x", "y", NOW)).isInstanceOf(ApiException.class);
  }

  @Test
  void cannotVerifyBeforeStartingVerification() {
    DsrRequest r = request(DsrType.ACCESS);
    assertThatThrownBy(() -> r.markVerified("dpo1", NOW)).isInstanceOf(ApiException.class);
  }

  @Test
  void rejectionIsTerminal() {
    DsrRequest r = request(DsrType.ERASURE);
    r.reject("could not verify identity", NOW);
    assertThat(r.getStatus()).isEqualTo("REJECTED");
    assertThatThrownBy(() -> r.startVerification()).isInstanceOf(ApiException.class);
  }

  @Test
  void overdueOnlyWhileNonTerminal() {
    DsrRequest r = request(DsrType.ACCESS);
    assertThat(r.isOverdue(DUE.plusSeconds(1))).isTrue();
    r.startVerification();
    r.markVerified("dpo1", NOW);
    r.complete("e", "done", NOW);
    assertThat(r.isOverdue(DUE.plusSeconds(1))).isFalse();
  }
}
