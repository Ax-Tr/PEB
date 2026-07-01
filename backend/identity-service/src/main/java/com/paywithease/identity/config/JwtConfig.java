package com.paywithease.identity.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Signing material for access tokens. Identity is the token issuer and also validates its own
 * tokens as a resource server; the gateway validates via the published JWKS. In production the RSA
 * key is KMS/Vault-managed and rotated — here it is generated at startup (single-instance dev) with
 * a warning. The private key never leaves this service; only the public JWK is exposed.
 */
@Configuration
public class JwtConfig {

  private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
  public static final String KEY_ID = "peb-identity-rs256";

  @Bean
  public RSAKey rsaKey() throws Exception {
    log.warn(
        "SECURITY: generating an ephemeral RSA signing key. Provide a KMS/Vault-managed, rotated "
            + "key and share it across identity instances before any non-local deployment.");
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair pair = generator.generateKeyPair();
    return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
        .privateKey(pair.getPrivate())
        .keyID(KEY_ID)
        .build();
  }

  @Bean
  public JWKSet jwkSet(RSAKey rsaKey) {
    return new JWKSet(rsaKey.toPublicJWK());
  }

  @Bean
  public JwtEncoder jwtEncoder(RSAKey rsaKey) {
    JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(rsaKey));
    return new NimbusJwtEncoder(source);
  }

  @Bean
  public JwtDecoder jwtDecoder(RSAKey rsaKey) throws Exception {
    return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
  }
}
