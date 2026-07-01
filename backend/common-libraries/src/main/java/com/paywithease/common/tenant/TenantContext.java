package com.paywithease.common.tenant;

import java.util.Optional;

/**
 * Holds the resolved tenant/business and acting user for the current request thread. Populated by
 * {@code TenantContextFilter} from gateway-forwarded headers and cleared at the end of the request.
 * Every tenant-scoped query must filter by {@link #requireTenantId()}.
 */
public final class TenantContext {

  public record Principal(
      String tenantId, String businessId, String actorId, String correlationId) {}

  private static final ThreadLocal<Principal> CURRENT = new ThreadLocal<>();

  private TenantContext() {}

  public static void set(Principal principal) {
    CURRENT.set(principal);
  }

  public static Optional<Principal> current() {
    return Optional.ofNullable(CURRENT.get());
  }

  public static String requireTenantId() {
    Principal p = CURRENT.get();
    if (p == null || p.tenantId() == null) {
      throw new IllegalStateException("No tenant in context; request is not tenant-resolved");
    }
    return p.tenantId();
  }

  public static Optional<String> actorId() {
    return current().map(Principal::actorId);
  }

  public static void clear() {
    CURRENT.remove();
  }
}
