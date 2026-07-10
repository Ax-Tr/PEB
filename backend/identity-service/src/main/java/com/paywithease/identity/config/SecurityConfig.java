package com.paywithease.identity.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless resource-server security. Auth entrypoints (OTP request/verify, token refresh) and the
 * JWKS/health/docs are public; everything else needs a valid access token. Authorities are derived
 * from the token's {@code roles} claim ({@code ROLE_*}) and {@code scope} ({@code SCOPE_*}) for
 * RBAC/ABAC method security.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private static final String[] PUBLIC = {
    "/api/v1/auth/otp/request",
    "/api/v1/auth/otp/verify",
    "/api/v1/auth/token/refresh",
    "/api/v1/ping",
    "/oauth2/jwks",
    "/.well-known/jwks.json",
    "/actuator/health/**",
    "/actuator/info",
    "/actuator/prometheus",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/v3/api-docs/**"
  };

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(request -> {
          var config = new org.springframework.web.cors.CorsConfiguration();
          config.setAllowedOrigins(List.of("*"));
          config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
          config.setAllowedHeaders(List.of("*"));
          return config;
        }))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth -> auth.requestMatchers(PUBLIC).permitAll().anyRequest().authenticated())
        .oauth2ResourceServer(
            oauth ->
                oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
    return http.build();
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(SecurityConfig::extractAuthorities);
    return converter;
  }

  private static Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    Collection<GrantedAuthority> authorities = new ArrayList<>();
    Object roles = jwt.getClaim("roles");
    if (roles instanceof List<?> list) {
      for (Object role : list) {
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
      }
    }
    String scope = jwt.getClaimAsString("scope");
    if (scope != null && !scope.isBlank()) {
      for (String s : scope.split(" ")) {
        authorities.add(new SimpleGrantedAuthority("SCOPE_" + s));
      }
    }
    return authorities;
  }

  /** Helper for ABAC checks that a request stays within the caller's tenant. */
  public static AuthorizationDecision sameTenant(Jwt jwt, String tenantId) {
    return new AuthorizationDecision(
        tenantId != null && tenantId.equals(jwt.getClaimAsString("tenant_id")));
  }
}
