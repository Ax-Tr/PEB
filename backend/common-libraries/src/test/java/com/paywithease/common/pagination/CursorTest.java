package com.paywithease.common.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CursorTest {

  @Test
  void roundTripsSortValueAndId() {
    String token = Cursor.encode("2026-07-01T00:00:00Z", "01H000000000000000000000AB");
    Cursor.Position p = Cursor.decode(token);
    assertThat(p.sortValue()).isEqualTo("2026-07-01T00:00:00Z");
    assertThat(p.id()).isEqualTo("01H000000000000000000000AB");
  }

  @Test
  void nullOrBlankTokenMeansFirstPage() {
    assertThat(Cursor.decode(null)).isNull();
    assertThat(Cursor.decode("  ")).isNull();
  }

  @Test
  void handlesNullSortValue() {
    String token = Cursor.encode(null, "id1");
    Cursor.Position p = Cursor.decode(token);
    assertThat(p.sortValue()).isNull();
    assertThat(p.id()).isEqualTo("id1");
  }

  @Test
  void encodeRequiresId() {
    assertThatThrownBy(() -> Cursor.encode("x", null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void malformedTokenIsRejected() {
    assertThatThrownBy(() -> Cursor.decode("!!!not-base64!!!"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void valuesContainingSeparatorLikeCharactersSurvive() {
    // sort values may contain colons, dashes, etc.
    String token = Cursor.encode("A:B-C_D 2026", "id-9");
    Cursor.Position p = Cursor.decode(token);
    assertThat(p.sortValue()).isEqualTo("A:B-C_D 2026");
    assertThat(p.id()).isEqualTo("id-9");
  }
}
