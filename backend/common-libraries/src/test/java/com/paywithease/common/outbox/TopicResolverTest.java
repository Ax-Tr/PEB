package com.paywithease.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Locks event→topic routing so it stays aligned with each consumer's @KafkaListener topics. */
class TopicResolverTest {

  private final TopicResolver r = TopicResolver.defaultResolver();

  @Test
  void routesPayoutFamilyBeforeMasterData() {
    // The critical disambiguation: VENDOR_PAYMENT_* is payout, VENDOR_CREATED is master data.
    assertThat(r.topicFor("VENDOR_PAYMENT_COMPLETED")).isEqualTo("payout.events");
    assertThat(r.topicFor("PAYOUT_APPROVAL_REQUESTED")).isEqualTo("payout.events");
    assertThat(r.topicFor("VENDOR_CREATED")).isEqualTo("masters.events");
    assertThat(r.topicFor("VENDOR_BANK_DETAILS_CHANGED")).isEqualTo("masters.events");
  }

  @Test
  void routesEachFamilyToItsConsumerTopic() {
    assertThat(r.topicFor("PAYMENT_RECEIVED")).isEqualTo("payment.events");
    assertThat(r.topicFor("INVOICE_GENERATED")).isEqualTo("invoice.events");
    assertThat(r.topicFor("PURCHASE_BILL_CREATED")).isEqualTo("purchase.events");
    assertThat(r.topicFor("EXPENSE_APPROVED")).isEqualTo("purchase.events");
    assertThat(r.topicFor("SALARY_RUN_CREATED")).isEqualTo("payroll.events");
    assertThat(r.topicFor("PAYSLIP_GENERATED")).isEqualTo("payroll.events");
    assertThat(r.topicFor("BANK_TRANSACTION_IMPORTED")).isEqualTo("ingestion.events");
    assertThat(r.topicFor("TRANSACTION_CLASSIFIED")).isEqualTo("ingestion.events");
    assertThat(r.topicFor("RECONCILIATION_MATCHED")).isEqualTo("reconciliation.events");
    assertThat(r.topicFor("REMINDER_SENT")).isEqualTo("notification.events");
    assertThat(r.topicFor("INSTALLMENT_PAID")).isEqualTo("installment.events");
    assertThat(r.topicFor("COMMITMENT_CREATED")).isEqualTo("commitment.events");
    assertThat(r.topicFor("OCR_REVIEW_REQUIRED")).isEqualTo("ocr.events");
    assertThat(r.topicFor("VOICE_DRAFT_CREATED")).isEqualTo("ai.events");
    assertThat(r.topicFor("AI_SUGGESTION_CREATED")).isEqualTo("ai.events");
    assertThat(r.topicFor("JOURNAL_ENTRY_POSTED")).isEqualTo("ledger.events");
    assertThat(r.topicFor("BUSINESS_CREATED")).isEqualTo("tenant.events");
    assertThat(r.topicFor("USER_LOGGED_IN")).isEqualTo("identity.events");
    assertThat(r.topicFor("CUSTOMER_CREATED")).isEqualTo("masters.events");
    assertThat(r.topicFor("SOMETHING_ELSE")).isEqualTo("peb.misc.events");
  }
}
