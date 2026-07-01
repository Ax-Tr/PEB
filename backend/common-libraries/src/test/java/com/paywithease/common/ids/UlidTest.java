package com.paywithease.common.ids;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UlidTest {

  @Test
  void generatesValidSortableIds() {
    String a = Ulid.newId();
    String b = Ulid.newId();
    assertThat(a).hasSize(26);
    assertThat(Ulid.isValid(a)).isTrue();
    assertThat(a.compareTo(b)).isLessThanOrEqualTo(0); // monotonic
  }

  @Test
  void rejectsInvalid() {
    assertThat(Ulid.isValid("nope")).isFalse();
    assertThat(Ulid.isValid(null)).isFalse();
    assertThatThrownBy(() -> Ulid.requireValid("bad")).isInstanceOf(IllegalArgumentException.class);
  }
}
