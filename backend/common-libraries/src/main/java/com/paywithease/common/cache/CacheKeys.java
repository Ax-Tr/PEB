package com.paywithease.common.cache;

/**
 * Builds cache keys that are always tenant-scoped, so one tenant's cached data can never be served
 * to another (cache poisoning / cross-tenant leakage is a classic multi-tenant bug). Every
 * application cache key must be built through here.
 *
 * <p>Format: {@code peb:{namespace}:t:{tenantId}:{part}:{part}...}. Parts are sanitised so a value
 * containing the delimiter cannot forge a different key.
 */
public final class CacheKeys {

  private static final String ROOT = "peb";
  private static final String DELIM = ":";

  private CacheKeys() {}

  /**
   * A tenant-scoped key. {@code tenantId} is mandatory — a null/blank tenant is a programming
   * error.
   */
  public static String tenant(String namespace, String tenantId, String... parts) {
    if (tenantId == null || tenantId.isBlank()) {
      throw new IllegalArgumentException("tenantId is required for a tenant-scoped cache key");
    }
    StringBuilder sb =
        new StringBuilder(ROOT)
            .append(DELIM)
            .append(clean(namespace))
            .append(DELIM)
            .append("t")
            .append(DELIM)
            .append(clean(tenantId));
    for (String part : parts) {
      sb.append(DELIM).append(clean(part));
    }
    return sb.toString();
  }

  /** A deliberately global (non-tenant) key — use only for genuinely shared reference data. */
  public static String global(String namespace, String... parts) {
    StringBuilder sb =
        new StringBuilder(ROOT)
            .append(DELIM)
            .append("global")
            .append(DELIM)
            .append(clean(namespace));
    for (String part : parts) {
      sb.append(DELIM).append(clean(part));
    }
    return sb.toString();
  }

  /** Replace the delimiter (and whitespace) so a value can't inject extra key segments. */
  private static String clean(String value) {
    if (value == null) {
      return "_";
    }
    return value.replace(DELIM, "_").replaceAll("\\s+", "_");
  }
}
