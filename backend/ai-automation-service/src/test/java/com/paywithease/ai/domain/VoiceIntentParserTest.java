package com.paywithease.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class VoiceIntentParserTest {

  private final VoiceIntentParser parser =
      new VoiceIntentParser(Clock.fixed(Instant.parse("2026-07-03T00:00:00Z"), ZoneOffset.UTC));

  @Test
  void parsesCommitmentAmountCounterpartyAndFridayDueDate() {
    ParsedVoiceIntent parsed = parser.parse("Raj promised to pay 5000 on Friday");

    assertThat(parsed.intent()).isEqualTo(VoiceIntent.CREATE_COMMITMENT);
    assertThat(parsed.fields()).containsEntry("counterpartyName", "Raj");
    assertThat(parsed.fields()).containsEntry("amountMinor", 500000L);
    assertThat(parsed.fields()).containsEntry("dueDate", "2026-07-03");
    assertThat(parsed.missingFields()).isEmpty();
    assertThat(parsed.confidence()).isGreaterThan(0.90);
  }

  @Test
  void incompleteCommitmentReportsMissingFields() {
    ParsedVoiceIntent parsed = parser.parse("Raj promised to pay");

    assertThat(parsed.intent()).isEqualTo(VoiceIntent.CREATE_COMMITMENT);
    assertThat(parsed.missingFields()).contains("amountMinor", "dueDate");
    assertThat(parsed.confidence()).isLessThan(0.70);
  }
}
