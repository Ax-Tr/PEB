package com.paywithease.ocr.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BankDetailExtractorTest {

  private final BankDetailExtractor extractor = new BankDetailExtractor();

  @Test
  void extractsIndianBankDetailsFromOcrText() {
    BankDetailExtractor.BankDetailExtraction result =
        extractor.extract(
            """
            HDFC Bank
            Account No: 50100123456789
            IFSC: HDFC0001234
            Holder: RAHUL SHARMA
            UPI rahul.sharma@okhdfcbank
            """);

    assertThat(result.fields().get("accountNumber").value()).isEqualTo("50100123456789");
    assertThat(result.fields().get("ifsc").value()).isEqualTo("HDFC0001234");
    assertThat(result.fields().get("upi").value()).isEqualTo("rahul.sharma@okhdfcbank");
    assertThat(result.fields().get("holderName").value()).isEqualTo("RAHUL SHARMA");
    assertThat(result.fields().get("bankName").value()).isEqualTo("Hdfc Bank");
    assertThat(result.confidence()).isGreaterThan(BigDecimal.valueOf(0.80));
  }
}
