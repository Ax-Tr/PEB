package com.paywithease.cacollaboration.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CloseChecklistTest {

  private CloseChecklistItem item(String id, boolean mandatory, boolean done) {
    CloseChecklistItem i = new CloseChecklistItem(id, "c1", "t1", "label " + id, mandatory, 0);
    if (done) {
      i.setDone(true, "actor1", java.time.Instant.parse("2026-07-01T00:00:00Z"));
    }
    return i;
  }

  @Test
  void locksOnlyWhenAllMandatoryDone() {
    assertThat(
            CloseChecklist.canLockMonth(
                List.of(item("a", true, true), item("b", true, true), item("c", false, false))))
        .isTrue();
  }

  @Test
  void blockedWhenAnyMandatoryUndone() {
    assertThat(CloseChecklist.canLockMonth(List.of(item("a", true, true), item("b", true, false))))
        .isFalse();
  }

  @Test
  void blockedWhenNoMandatoryItems() {
    assertThat(CloseChecklist.canLockMonth(List.of(item("a", false, true)))).isFalse();
    assertThat(CloseChecklist.canLockMonth(List.of())).isFalse();
  }
}
