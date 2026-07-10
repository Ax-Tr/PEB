package com.paywithease.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Edge authentication. The gateway validates access tokens (JWKS published by identity-service) and
 * rejects unauthenticated calls to protected routes before they reach any service. Auth bootstrap
 * endpoints (OTP request/verify, token refresh), health, and docs are open.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

  private static final String[] PUBLIC = {
    "/api/v1/auth/otp/request",
    "/api/v1/auth/otp/verify",
    "/api/v1/auth/token/refresh",
    "/api/v1/ping",
    "/api/v1/webhooks/payments/**",
    "/api/v1/webhooks/notifications/**",
    "/fallback/**",
    "/actuator/health/**",
    "/actuator/info",
    "/actuator/prometheus"
  };

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.addAllowedOriginPattern("*");
    configuration.addAllowedMethod("*");
    configuration.addAllowedHeader("*");
    configuration.setAllowCredentials(false);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    http.cors(Customizer.withDefaults())
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(
            ex ->
                ex.pathMatchers(HttpMethod.OPTIONS)
                    .permitAll()
                    .pathMatchers(PUBLIC)
                    .permitAll()
                    .anyExchange()
                    .authenticated())
        .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}));
    return http.build();
  }
}
