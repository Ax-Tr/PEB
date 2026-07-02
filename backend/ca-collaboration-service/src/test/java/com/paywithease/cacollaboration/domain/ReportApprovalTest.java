package com.paywithease.cacollaboration.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paywithease.common.error.ApiException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReportApprovalTest {

  private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

  private ReportApproval approval() {
    return new ReportApproval("a1", "t1", "GSTR3B_SUMMARY", "rep1", "maker1", NOW);
  }

  @Test
  void differentApproverCanApprove() {
    ReportApproval a = approval();
    a.decide("checker1", true, "looks good", NOW);
    assertThat(a.getStatus()).isEqualTo("APPROVED");
    assertThat(a.getDecidedBy()).isEqualTo("checker1");
  }

  @Test
  void requesterCannotApproveOwnRequest() {
    ReportApproval a = approval();
    assertThatThrownBy(() -> a.decide("maker1", true, null, NOW))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("Maker-checker");
  }

  @Test
  void cannotDecideTwice() {
    ReportApproval a = approval();
    a.decide("checker1", true, null, NOW);
    assertThatThrownBy(() -> a.decide("checker2", false, null, NOW))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("already decided");
  }

  @Test
  void rejectionIsRecorded() {
    ReportApproval a = approval();
    a.decide("checker1", false, "mismatch in ITC", NOW);
    assertThat(a.getStatus()).isEqualTo("REJECTED");
  }
}
