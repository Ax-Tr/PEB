package com.paywithease.reconciliation.domain;

import java.util.HashSet;
import java.util.Set;

/** Token Jaccard similarity (0..1) for fuzzy counterparty/narration comparison. */
public final class StringSimilarity {

  private StringSimilarity() {}

  public static double jaccard(String a, String b) {
    Set<String> ta = tokens(a);
    Set<String> tb = tokens(b);
    if (ta.isEmpty() || tb.isEmpty()) {
      return 0.0;
    }
    Set<String> intersection = new HashSet<>(ta);
    intersection.retainAll(tb);
    Set<String> union = new HashSet<>(ta);
    union.addAll(tb);
    return (double) intersection.size() / union.size();
  }

  private static Set<String> tokens(String s) {
    Set<String> out = new HashSet<>();
    if (s == null) {
      return out;
    }
    for (String t : s.toLowerCase().split("[^a-z0-9]+")) {
      if (t.length() > 1) {
        out.add(t);
      }
    }
    return out;
  }
}
