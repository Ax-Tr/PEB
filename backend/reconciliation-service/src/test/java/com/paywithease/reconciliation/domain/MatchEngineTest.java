package com.paywithease.reconciliation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatchEngineTest {

  private static final LocalDate D = LocalDate.of(2026, 7, 1);

  private MatchEngine.Item item(
      String id, String dir, long amt, LocalDate date, String ref, String cp, String narr) {
    return new MatchEngine.Item(id, dir, amt, date, ref, cp, narr);
  }

  @Test
  void exactAmountReferenceAndDateAutoMatches() {
    var ext = item("e1", "CREDIT", 118000, D, "UTR123", "Rahul Sharma", "UPI/UTR123/Rahul");
    var cand = item("i1", "CREDIT", 118000, D, "UTR123", "Rahul Sharma", "Invoice INV1");
    var r = MatchEngine.match(ext, List.of(cand));
    assertThat(r.decision()).isEqualTo(MatchEngine.Decision.AUTO);
    assertThat(r.candidateId()).isEqualTo("i1");
    assertThat(r.score()).isGreaterThanOrEqualTo(0.90);
  }

  @Test
  void amountDateAndCounterpartySuggested() {
    var ext = item("e1", "CREDIT", 118000, D, null, "Acme Traders", "neft credit");
    var cand = item("i1", "CREDIT", 118000, D, null, "Acme Traders", "invoice");
    var r = MatchEngine.match(ext, List.of(cand));
    assertThat(r.decision()).isEqualTo(MatchEngine.Decision.SUGGESTED); // 0.45 + 0.10 + 0.05 = 0.60
    assertThat(r.candidateId()).isEqualTo("i1");
  }

  @Test
  void amountMismatchIsException() {
    var ext = item("e1", "CREDIT", 118000, D, "UTR9", "x", "y");
    var cand =
        item("i1", "CREDIT", 100000, D, "UTR9", "x", "y"); // ref+date match but amount differs
    var r = MatchEngine.match(ext, List.of(cand));
    assertThat(r.decision()).isEqualTo(MatchEngine.Decision.EXCEPTION);
    assertThat(r.candidateId()).isNull();
  }

  @Test
  void directionMismatchDisqualifies() {
    var ext = item("e1", "CREDIT", 118000, D, "UTR1", "x", "y");
    var cand = item("i1", "DEBIT", 118000, D, "UTR1", "x", "y");
    assertThat(MatchEngine.score(ext, cand)).isZero();
    assertThat(MatchEngine.match(ext, List.of(cand)).decision())
        .isEqualTo(MatchEngine.Decision.EXCEPTION);
  }

  @Test
  void noCandidatesIsException() {
    var ext = item("e1", "CREDIT", 118000, D, "UTR1", "x", "y");
    var r = MatchEngine.match(ext, List.of());
    assertThat(r.decision()).isEqualTo(MatchEngine.Decision.EXCEPTION);
    assertThat(r.score()).isZero();
  }

  @Test
  void picksBestOfSeveralCandidates() {
    var ext = item("e1", "CREDIT", 118000, D, "UTR123", "Rahul", "UTR123");
    var weak = item("w", "CREDIT", 118000, D.minusDays(4), null, "someone", "misc");
    var strong = item("s", "CREDIT", 118000, D, "UTR123", "Rahul", "x");
    var r = MatchEngine.match(ext, List.of(weak, strong));
    assertThat(r.candidateId()).isEqualTo("s");
    assertThat(r.decision()).isEqualTo(MatchEngine.Decision.AUTO);
  }
}
