package com.paywithease.common.correlation;

import com.paywithease.common.ids.Ulid;
import com.paywithease.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Establishes the correlation id + tenant context for every request and puts them in the SLF4J MDC
 * so structured logs carry {@code correlationId}/{@code tenantId}. Headers are forwarded by the API
 * gateway after authentication (never trusted directly from external clients without the gateway).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationFilter extends OncePerRequestFilter {

  public static final String CORRELATION_HEADER = "X-Correlation-Id";
  public static final String TENANT_HEADER = "X-Tenant-Id";
  public static final String BUSINESS_HEADER = "X-Business-Id";
  public static final String ACTOR_HEADER = "X-Actor-Id";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String correlationId = header(request, CORRELATION_HEADER, Ulid.newId());
    String tenantId = header(request, TENANT_HEADER, null);
    String businessId = header(request, BUSINESS_HEADER, tenantId);
    String actorId = header(request, ACTOR_HEADER, null);

    TenantContext.set(new TenantContext.Principal(tenantId, businessId, actorId, correlationId));
    MDC.put("correlationId", correlationId);
    if (tenantId != null) {
      MDC.put("tenantId", tenantId);
    }
    response.setHeader(CORRELATION_HEADER, correlationId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.clear();
      TenantContext.clear();
    }
  }

  private static String header(HttpServletRequest request, String name, String fallback) {
    String value = request.getHeader(name);
    return (value == null || value.isBlank()) ? fallback : value;
  }
}
