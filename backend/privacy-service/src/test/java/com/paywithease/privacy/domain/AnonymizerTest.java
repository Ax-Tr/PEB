package com.paywithease.privacy.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnonymizerTest {

  @Test
  void emailKeepsDomainButRemovesLocalPart() {
    assertThat(Anonymizer.email("ravi.kumar@acme.co.in")).isEqualTo("redacted@acme.co.in");
    assertThat(Anonymizer.email("not-an-email")).isEqualTo(Anonymizer.REDACTED);
  }

  @Test
  void phoneAndPanArePreservedInShapeButMasked() {
    assertThat(Anonymizer.phone("9876543210")).isEqualTo("XXXXXXXXXX");
    assertThat(Anonymizer.pan("ABCDE1234F")).isEqualTo("XXXXX0000X");
  }

  @Test
  void pseudonymIsStableAndSaltDependent() {
    String a = Anonymizer.pseudonym("user-123", "salt1");
    String b = Anonymizer.pseudonym("user-123", "salt1");
    String c = Anonymizer.pseudonym("user-123", "salt2");
    assertThat(a).isEqualTo(b).startsWith("anon_");
    assertThat(a).isNotEqualTo(c); // different salt -> different pseudonym
  }

  @Test
  void nullsAreHandledSafely() {
    assertThat(Anonymizer.name(null)).isNull();
    assertThat(Anonymizer.phone(null)).isNull();
    assertThat(Anonymizer.pseudonym(null, "s")).isNull();
  }
}
