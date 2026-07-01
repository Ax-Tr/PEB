package com.paywithease.gateway;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Tenant resolution at the edge: after the token is validated, forwards the authenticated user id
 * and tenant claim to downstream services as {@code X-Actor-Id} / {@code X-Tenant-Id}. Services
 * trust these headers because they only ever arrive via the gateway (internal mesh is mTLS). Any
 * such headers on the inbound request are stripped first to prevent spoofing.
 */
@Component
public class TenantHeaderGlobalFilter implements GlobalFilter, Ordered {

  private static final String TENANT_HEADER = "X-Tenant-Id";
  private static final String ACTOR_HEADER = "X-Actor-Id";
  private static final String BUSINESS_HEADER = "X-Business-Id";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    // Strip any client-supplied identity headers up front (anti-spoofing).
    ServerWebExchange stripped =
        exchange
            .mutate()
            .request(
                r ->
                    r.headers(
                        h -> {
                          h.remove(TENANT_HEADER);
                          h.remove(ACTOR_HEADER);
                          h.remove(BUSINESS_HEADER);
                        }))
            .build();

    return stripped
        .getPrincipal()
        .filter(JwtAuthenticationToken.class::isInstance)
        .cast(JwtAuthenticationToken.class)
        .map(JwtAuthenticationToken::getToken)
        .map(
            (Jwt jwt) -> {
              String tenantId = jwt.getClaimAsString("tenant_id");
              return stripped
                  .mutate()
                  .request(
                      r ->
                          r.headers(
                              h -> {
                                h.set(ACTOR_HEADER, jwt.getSubject());
                                if (tenantId != null && !tenantId.isBlank()) {
                                  h.set(TENANT_HEADER, tenantId);
                                  h.set(BUSINESS_HEADER, tenantId);
                                }
                              }))
                  .build();
            })
        .defaultIfEmpty(stripped)
        .flatMap(chain::filter);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 1; // after correlation id is set
  }
}
