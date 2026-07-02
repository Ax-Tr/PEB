package com.paywithease.notification.domain;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal {@code {{placeholder}}} template renderer (no external dependency). Known variables are
 * substituted; unknown placeholders render as empty so a customer never sees raw {{tokens}}.
 */
public final class TemplateEngine {

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*}}");

  private TemplateEngine() {}

  public static String render(String template, Map<String, String> variables) {
    if (template == null || template.isEmpty()) {
      return template;
    }
    Map<String, String> vars = variables == null ? Map.of() : variables;
    Matcher matcher = PLACEHOLDER.matcher(template);
    StringBuilder out = new StringBuilder();
    while (matcher.find()) {
      String key = matcher.group(1);
      String value = vars.getOrDefault(key, "");
      matcher.appendReplacement(out, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(out);
    return out.toString();
  }
}
