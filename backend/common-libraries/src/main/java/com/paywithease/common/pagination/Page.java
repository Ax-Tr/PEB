package com.paywithease.common.pagination;

import java.util.List;
import java.util.function.Function;

/**
 * A single page of keyset-paginated results plus the cursor to fetch the next page ({@code null}
 * when there are no more). Build it from one extra-fetched row so {@code hasMore} is known without
 * a second count query.
 *
 * @param <T> the item type returned to the caller
 */
public record Page<T>(List<T> items, String nextCursor, boolean hasMore) {

  /**
   * Build a page from a list fetched with {@code limit + 1} rows. If the fetch returned more than
   * {@code limit}, there is another page and the surplus row is dropped; the next cursor is derived
   * from the last kept row via {@code sortValueOf}/{@code idOf}.
   *
   * @param fetched rows fetched with size up to {@code limit + 1}
   * @param limit the requested page size
   */
  public static <T> Page<T> of(
      List<T> fetched, int limit, Function<T, String> sortValueOf, Function<T, String> idOf) {
    if (limit <= 0) {
      throw new IllegalArgumentException("limit must be positive");
    }
    boolean hasMore = fetched.size() > limit;
    List<T> items = hasMore ? List.copyOf(fetched.subList(0, limit)) : List.copyOf(fetched);
    String nextCursor = null;
    if (hasMore && !items.isEmpty()) {
      T last = items.get(items.size() - 1);
      nextCursor = Cursor.encode(sortValueOf.apply(last), idOf.apply(last));
    }
    return new Page<>(items, nextCursor, hasMore);
  }

  /** An empty page with no further results. */
  public static <T> Page<T> empty() {
    return new Page<>(List.of(), null, false);
  }
}
