package com.paywithease.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateEngineTest {

  @Test
  void substitutesKnownPlaceholders() {
    String out =
        TemplateEngine.render(
            "Hi {{name}}, EMI of {{amount}} is due on {{dueDate}}.",
            Map.of("name", "Rahul", "amount", "₹4,000.00", "dueDate", "01 Jul 2026"));
    assertThat(out).isEqualTo("Hi Rahul, EMI of ₹4,000.00 is due on 01 Jul 2026.");
  }

  @Test
  void unknownPlaceholdersRenderEmpty() {
    assertThat(TemplateEngine.render("Hi {{name}}{{missing}}!", Map.of("name", "A")))
        .isEqualTo("Hi A!");
  }

  @Test
  void handlesWhitespaceAndNulls() {
    assertThat(TemplateEngine.render("{{ name }}", Map.of("name", "X"))).isEqualTo("X");
    assertThat(TemplateEngine.render(null, Map.of())).isNull();
    assertThat(TemplateEngine.render("no vars", null)).isEqualTo("no vars");
  }
}
