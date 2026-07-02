package com.paywithease.ingestion.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TransactionClassifierTest {

  @Test
  void matchesStrongKeywordsWithHighConfidence() {
    assertThat(TransactionClassifier.classify("SALARY JUNE", Direction.DEBIT).category())
        .isEqualTo("SALARY");
    assertThat(TransactionClassifier.classify("GST PAYMENT CGST", Direction.DEBIT).category())
        .isEqualTo("GST_TAX");
    assertThat(TransactionClassifier.classify("HDFC LOAN EMI", Direction.DEBIT).category())
        .isEqualTo("LOAN_EMI");
    assertThat(TransactionClassifier.classify("ATM CASH WD", Direction.DEBIT).confidence())
        .isGreaterThanOrEqualTo(new BigDecimal("0.80"));
  }

  @Test
  void creditWithoutKeywordIsSalesLowConfidence() {
    var s = TransactionClassifier.classify("NEFT FROM CUSTOMER", Direction.CREDIT);
    // NEFT keyword matches TRANSFER before the credit fallback
    assertThat(s.category()).isEqualTo("TRANSFER");

    var plain = TransactionClassifier.classify("misc credit", Direction.CREDIT);
    assertThat(plain.category()).isEqualTo("SALES");
    assertThat(plain.confidence()).isEqualByComparingTo("0.50");
  }

  @Test
  void unknownDebitIsLowConfidenceAndNeedsReview() {
    var s = TransactionClassifier.classify("random purchase", Direction.DEBIT);
    assertThat(s.category()).isEqualTo("UNKNOWN");
    assertThat(s.confidence()).isEqualByComparingTo("0.30"); // low → must be reviewed
  }
}
