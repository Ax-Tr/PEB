package com.paywithease.common.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PageTest {

  private record Row(String id, String sortValue) {}

  @Test
  void detectsMorePagesFromExtraRowAndEmitsCursor() {
    // limit 2, fetched 3 (limit+1) -> hasMore, drop surplus, cursor from last kept row
    List<Row> fetched = List.of(new Row("a", "t3"), new Row("b", "t2"), new Row("c", "t1"));
    Page<Row> page = Page.of(fetched, 2, Row::sortValue, Row::id);
    assertThat(page.items()).extracting(Row::id).containsExactly("a", "b");
    assertThat(page.hasMore()).isTrue();
    assertThat(Cursor.decode(page.nextCursor()).id()).isEqualTo("b");
    assertThat(Cursor.decode(page.nextCursor()).sortValue()).isEqualTo("t2");
  }

  @Test
  void lastPageHasNoCursor() {
    List<Row> fetched = List.of(new Row("a", "t3"), new Row("b", "t2"));
    Page<Row> page = Page.of(fetched, 2, Row::sortValue, Row::id);
    assertThat(page.items()).hasSize(2);
    assertThat(page.hasMore()).isFalse();
    assertThat(page.nextCursor()).isNull();
  }

  @Test
  void emptyResult() {
    Page<Row> page = Page.of(List.of(), 10, Row::sortValue, Row::id);
    assertThat(page.items()).isEmpty();
    assertThat(page.hasMore()).isFalse();
    assertThat(Page.<Row>empty().nextCursor()).isNull();
  }
}
