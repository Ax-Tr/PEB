package com.paywithease.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiiMaskingConverterTest {

  @Test
  void masksMobileKeepingLastTwo() {
    assertThat(PiiMaskingConverter.mask("call 9876543210 now")).isEqualTo("call ********10 now");
  }

  @Test
  void masksPanAndGstin() {
    assertThat(PiiMaskingConverter.mask("PAN ABCDE1234F")).contains("PAN_****");
    assertThat(PiiMaskingConverter.mask("gst 29ABCDE1234F1Z5")).contains("GSTIN_****");
  }

  @Test
  void masksIfscAndAccount() {
    assertThat(PiiMaskingConverter.mask("HDFC0001234")).contains("IFSC_****");
    assertThat(PiiMaskingConverter.mask("acc 123456789012")).contains("********9012");
  }

  @Test
  void masksEmail() {
    assertThat(PiiMaskingConverter.mask("mail rahul@example.com")).contains("r***@example.com");
  }

  @Test
  void nullAndEmptyPassThrough() {
    assertThat(PiiMaskingConverter.mask(null)).isNull();
    assertThat(PiiMaskingConverter.mask("")).isEmpty();
  }
}
