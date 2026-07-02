package com.paywithease.cacollaboration.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Resource-server security. Validates access tokens issued by identity-service via its published
 * JWKS ({@code spring.security.oauth2.resourceserver.jwt.jwk-set-uri}). Authorities are derived
 * from the token's roles/scope for RBAC method security; per-tenant ABAC is enforced in the service
 * layer via {@code TenantContext}. Collaboration write endpoints add maker-checker method security;
 * the read-only auditor collaborator scope is enforced by the service layer, not a Spring role.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private static final String[] PUBLIC = {
    "/actuator/health/**",
    "/actuator/info",
    "/actuator/prometheus",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/v3/api-docs/**"
  };

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(PUBLIC)
                    .permitAll()
                    .requestMatchers("/api/v1/collaboration/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
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
}
