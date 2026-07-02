package com.paywithease.common.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CacheKeysTest {

  @Test
  void tenantKeyIncludesTenantAndParts() {
    String key = CacheKeys.tenant("pnl", "tenant1", "2026", "05");
    assertThat(key).isEqualTo("peb:pnl:t:tenant1:2026:05");
  }

  @Test
  void differentTenantsNeverCollide() {
    String a = CacheKeys.tenant("pnl", "tenantA", "2026", "05");
    String b = CacheKeys.tenant("pnl", "tenantB", "2026", "05");
    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void tenantIdIsMandatory() {
    assertThatThrownBy(() -> CacheKeys.tenant("pnl", "  ", "x"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void delimiterInjectionCannotForgeAnotherTenantsKey() {
    // A crafted part must not be able to produce tenantB's key.
    String forged = CacheKeys.tenant("pnl", "tenantA", "x:t:tenantB");
    String real = CacheKeys.tenant("pnl", "tenantB");
    assertThat(forged).isNotEqualTo(real);
    assertThat(forged).doesNotContain(":t:tenantB"); // the colons were sanitised
  }

  @Test
  void globalKeyIsSeparateNamespace() {
    assertThat(CacheKeys.global("hsn-codes", "998311")).isEqualTo("peb:global:hsn-codes:998311");
  }
}
