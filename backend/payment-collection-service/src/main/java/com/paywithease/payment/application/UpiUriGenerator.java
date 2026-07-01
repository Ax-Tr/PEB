package com.paywithease.payment.application;

import com.paywithease.common.money.Money;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Builds a dynamic UPI intent URI (NPCI UPI deep-link spec) that is encoded into the QR / payment
 * link. Example: {@code upi://pay?pa=acme@upi&pn=Acme&am=100.00&cu=INR&tn=Order&tr=PEB01J...}. The
 * amount is the exact rupee value derived from integer paise; {@code tr} (transaction reference) is
 * our unique reference used to reconcile the inbound webhook.
 */
@Component
public class UpiUriGenerator {

  private final String linkBase;

  public UpiUriGenerator(
      @Value("${peb.payments.link-base:https://pay.paywithease.app/r/}") String linkBase) {
    this.linkBase = linkBase.endsWith("/") ? linkBase : linkBase + "/";
  }

  public String buildUpiUri(
      String payeeVpa, String payeeName, Money amount, String note, String reference) {
    StringBuilder sb = new StringBuilder("upi://pay?");
    sb.append("pa=").append(enc(payeeVpa));
    sb.append("&pn=").append(enc(payeeName));
    sb.append("&am=").append(enc(amount.toRupees().toPlainString()));
    sb.append("&cu=INR");
    if (note != null && !note.isBlank()) {
      sb.append("&tn=").append(enc(note));
    }
    sb.append("&tr=").append(enc(reference));
    return sb.toString();
  }

  public String buildPaymentLink(String reference) {
    return linkBase + reference;
  }

  private static String enc(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }
}
