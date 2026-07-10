package com.paywithease.gateway;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Ensures every request entering the mesh carries an {@code X-Correlation-Id}. Downstream services
 * read this header (see {@code CorrelationFilter} in common-libraries) to thread it through logs,
 * traces, and Kafka event headers. Runs first so all subsequent filters see the id.
 */
@Component
public class CorrelationGlobalFilter implements GlobalFilter, Ordered {

  public static final String CORRELATION_HEADER = "X-Correlation-Id";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String existing = exchange.getRequest().getHeaders().getFirst(CORRELATION_HEADER);
    String correlationId =
        (existing == null || existing.isBlank()) ? UUID.randomUUID().toString() : existing;

    ServerHttpRequest decorator =
        new ServerHttpRequestDecorator(exchange.getRequest()) {
          private HttpHeaders headers;

          @Override
          public HttpHeaders getHeaders() {
            if (headers == null) {
              headers = new HttpHeaders();
              headers.putAll(super.getHeaders());
              headers.set(CORRELATION_HEADER, correlationId);
            }
            return headers;
          }
        };

    ServerWebExchange mutatedExchange = exchange.mutate().request(decorator).build();
    mutatedExchange.getResponse().getHeaders().set(CORRELATION_HEADER, correlationId);

    return chain.filter(mutatedExchange);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
