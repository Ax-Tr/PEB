package com.paywithease.compliance.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paywithease.common.error.ApiException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ComplianceReportTest {

  private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

  private ComplianceReport report() {
    return new ComplianceReport("r1", "t1", ReportType.GSTR3B_SUMMARY, 2026, 5, NOW);
  }

  @Test
  void approveRequiresReconciledData() {
    ComplianceReport r = report();
    r.markReviewed("ca1");
    assertThatThrownBy(() -> r.approve("ca1"))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("reconciled");
  }

  @Test
  void happyPathDraftReviewApproveFile() {
    ComplianceReport r = report();
    r.markReviewed("ca1");
    r.setReconciled(true);
    r.approve("owner1");
    assertThat(r.getStatus()).isEqualTo("APPROVED");
    r.recordFiling("ACK-2026-05-0001", NOW);
    assertThat(r.getStatus()).isEqualTo("FILED");
    assertThat(r.getAckReference()).isEqualTo("ACK-2026-05-0001");
  }

  @Test
  void cannotFileWithoutAcknowledgement() {
    ComplianceReport r = report();
    r.markReviewed("ca1");
    r.setReconciled(true);
    r.approve("owner1");
    assertThatThrownBy(() -> r.recordFiling("  ", NOW))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("acknowledgement");
  }

  @Test
  void cannotFileBeforeApproval() {
    ComplianceReport r = report();
    assertThatThrownBy(() -> r.recordFiling("ACK1", NOW))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("approved");
  }

  @Test
  void displayStateShowsUnreconciledUntilReconciled() {
    ComplianceReport r = report();
    assertThat(r.displayState()).isEqualTo("UNRECONCILED");
    r.setReconciled(true);
    assertThat(r.displayState()).isEqualTo("DRAFT");
  }
}
