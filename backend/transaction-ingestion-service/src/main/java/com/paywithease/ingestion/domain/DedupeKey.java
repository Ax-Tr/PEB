package com.paywithease.ingestion.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;

/**
 * Computes the deterministic natural-key hash used to detect duplicate transactions on (re-)import.
 * When the source provides an external reference (UTR / UPI txn id / settlement id) that is the
 * key; otherwise it falls back to account + direction + amount + date + narration. Same inputs →
 * same hash, so a re-imported statement never creates duplicate rows.
 */
public final class DedupeKey {

  private DedupeKey() {}

  public static String compute(
      String tenantId,
      String bankAccountId,
      TxnSource source,
      Direction direction,
      long amountMinor,
      LocalDate txnDate,
      String externalRef,
      String narration) {
    String basis;
    if (externalRef != null && !externalRef.isBlank()) {
      basis = String.join("|", tenantId, nn(bankAccountId), source.name(), externalRef.trim());
    } else {
      basis =
          String.join(
              "|",
              tenantId,
              nn(bankAccountId),
              source.name(),
              direction.name(),
              Long.toString(amountMinor),
              String.valueOf(txnDate),
              normalize(narration));
    }
    return sha256Hex(basis);
  }

  private static String normalize(String s) {
    return s == null ? "" : s.trim().toUpperCase().replaceAll("\\s+", " ");
  }

  private static String nn(String s) {
    return s == null ? "" : s;
  }

  private static String sha256Hex(String value) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Dedupe hashing failed", e);
    }
  }
}
