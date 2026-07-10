package com.paywithease.identity.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.File;
import java.io.FileWriter;
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
 * key is KMS/Vault-managed and rotated. For local dev the key is persisted to a file in the build
 * directory so restarts don't invalidate existing tokens. The private key never leaves this
 * service; only the public JWK is exposed.
 */
@Configuration
public class JwtConfig {

  private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);
  public static final String KEY_ID = "peb-identity-rs256";

  /** Resolved path for the persisted local dev key (inside the Gradle build dir = gitignored). */
  private static final String DEV_KEY_FILE =
      System.getProperty("user.dir") + "/build/dev-jwt-signing-key.json";

  @Bean
  public RSAKey rsaKey() throws Exception {
    File keyFile = new File(DEV_KEY_FILE);
    if (keyFile.exists()) {
      try {
        JWKSet set = JWKSet.load(keyFile);
        RSAKey loaded = (RSAKey) set.getKeyByKeyId(KEY_ID);
        if (loaded != null && loaded.isPrivate()) {
          log.info("IDENTITY: loaded persistent dev JWT signing key from {}", DEV_KEY_FILE);
          return loaded;
        }
      } catch (Exception e) {
        log.warn(
            "IDENTITY: failed to load dev key from {}; generating a new one. Cause: {}",
            DEV_KEY_FILE,
            e.getMessage());
      }
    }

    log.warn(
        "SECURITY: generating an RSA signing key. Provide a KMS/Vault-managed, rotated "
            + "key before any non-local deployment.");
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair pair = generator.generateKeyPair();
    RSAKey key =
        new RSAKey.Builder((RSAPublicKey) pair.getPublic())
            .privateKey(pair.getPrivate())
            .keyID(KEY_ID)
            .build();

    // Persist for subsequent restarts
    keyFile.getParentFile().mkdirs();
    try (FileWriter writer = new FileWriter(keyFile)) {
      writer.write(new JWKSet(key).toJSONObject(false).toString());
      log.info("IDENTITY: persisted dev JWT signing key to {}", DEV_KEY_FILE);
    } catch (Exception e) {
      log.warn("IDENTITY: could not persist dev key to {}: {}", DEV_KEY_FILE, e.getMessage());
    }
    return key;
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
