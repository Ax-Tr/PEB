package com.paywithease.payment.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.paywithease.common.money.Money;
import org.junit.jupiter.api.Test;

class UpiUriGeneratorTest {

  private final UpiUriGenerator generator = new UpiUriGenerator("https://pay.example/r/");

  @Test
  void buildsUpiIntentUri() {
    String uri =
        generator.buildUpiUri(
            "acme@upi", "Acme Traders", Money.ofMinor(150000), "Order #1", "PEB123");
    assertThat(uri).startsWith("upi://pay?");
    assertThat(uri).contains("pa=acme%40upi");
    assertThat(uri).contains("pn=Acme+Traders");
    assertThat(uri).contains("am=1500.00"); // 150000 paise = ₹1500.00
    assertThat(uri).contains("cu=INR");
    assertThat(uri).contains("tn=Order+%231");
    assertThat(uri).contains("tr=PEB123");
  }

  @Test
  void omitsNoteWhenBlank() {
    String uri = generator.buildUpiUri("a@upi", "A", Money.ofMinor(100), null, "REF");
    assertThat(uri).doesNotContain("tn=");
    assertThat(uri).contains("am=1.00");
  }

  @Test
  void buildsPaymentLinkAndNormalizesBase() {
    assertThat(generator.buildPaymentLink("PEB123")).isEqualTo("https://pay.example/r/PEB123");
    UpiUriGenerator noSlash = new UpiUriGenerator("https://pay.example/r");
    assertThat(noSlash.buildPaymentLink("X")).isEqualTo("https://pay.example/r/X");
  }
}
