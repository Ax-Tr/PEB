package com.paywithease.identity.api;

import com.nimbusds.jose.jwk.JWKSet;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publishes the public JWKS so the gateway (and other resource servers) can validate access tokens.
 */
@RestController
public class JwksController {

  private final JWKSet jwkSet;

  public JwksController(JWKSet jwkSet) {
    this.jwkSet = jwkSet;
  }

  @GetMapping({"/oauth2/jwks", "/.well-known/jwks.json"})
  public Map<String, Object> keys() {
    return jwkSet.toJSONObject(); // public keys only
  }
}
