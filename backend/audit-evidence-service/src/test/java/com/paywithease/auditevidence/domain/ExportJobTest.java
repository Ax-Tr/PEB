package com.paywithease.auditevidence.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.paywithease.common.error.ApiException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ExportJobTest {

  private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

  private ExportJob job() {
    return new ExportJob("j1", "t1", "GSTR3B 2026-05", "auditor1", NOW);
  }

  @Test
  void happyPathRequestedProcessingCompleted() {
    ExportJob j = job();
    assertThat(j.getStatus()).isEqualTo("REQUESTED");
    j.start(NOW);
    assertThat(j.getStatus()).isEqualTo("PROCESSING");
    j.complete("s3://bundle.zip", NOW);
    assertThat(j.getStatus()).isEqualTo("COMPLETED");
    assertThat(j.getResultRef()).isEqualTo("s3://bundle.zip");
  }

  @Test
  void cannotCompleteBeforeProcessing() {
    ExportJob j = job();
    assertThatThrownBy(() -> j.complete("x", NOW)).isInstanceOf(ApiException.class);
  }

  @Test
  void canFailWhileProcessingButNotAfterComplete() {
    ExportJob j = job();
    j.start(NOW);
    j.fail("disk full", NOW);
    assertThat(j.getStatus()).isEqualTo("FAILED");

    ExportJob done = job();
    done.start(NOW);
    done.complete("ref", NOW);
    assertThatThrownBy(() -> done.fail("late", NOW)).isInstanceOf(ApiException.class);
  }
}
