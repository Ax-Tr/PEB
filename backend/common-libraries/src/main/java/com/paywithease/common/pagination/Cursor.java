package com.paywithease.common.pagination;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Opaque cursor for <b>keyset (seek) pagination</b>. At scale, offset/limit paging degrades
 * linearly (the database still scans and discards all skipped rows); keyset paging instead seeks
 * past the last row seen using its sort key, so page N costs the same as page 1.
 *
 * <p>A cursor encodes the composite sort key of the last returned row — a business sort value plus
 * the row id as a tie-breaker (ids are unique, guaranteeing a total order and stable paging). The
 * token is base64 of {@code sortValue|id}; it is opaque to clients and must not be parsed by them.
 */
public final class Cursor {

  private static final String SEPARATOR = ""; // control char unlikely to appear in values

  private Cursor() {}

  public record Position(String sortValue, String id) {}

  /** Encode the last row's (sortValue, id) into an opaque token. */
  public static String encode(String sortValue, String id) {
    if (id == null) {
      throw new IllegalArgumentException("id (tie-breaker) is required for a cursor");
    }
    String raw = (sortValue == null ? "" : sortValue) + SEPARATOR + id;
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  /** Decode a token; returns {@code null} for a null/blank token (i.e. request the first page). */
  public static Position decode(String token) {
    if (token == null || token.isBlank()) {
      return null;
    }
    String raw;
    try {
      raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Malformed page cursor");
    }
    int sep = raw.indexOf(SEPARATOR);
    if (sep < 0) {
      throw new IllegalArgumentException("Malformed page cursor");
    }
    String sortValue = raw.substring(0, sep);
    String id = raw.substring(sep + 1);
    return new Position(sortValue.isEmpty() ? null : sortValue, id);
  }
}
