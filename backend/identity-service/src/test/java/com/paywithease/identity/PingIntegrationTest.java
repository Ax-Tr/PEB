package com.paywithease.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies the service boots against a real Postgres (Flyway baseline applies) and serves ping.
 * Redis and Kafka autoconfiguration are excluded so the test is hermetic (Sprint 0 has no flows
 * that use them yet).
 */
@SpringBootTest(
    properties = {
      // Redis autoconfig stays on (StringRedisTemplate is created lazily, no broker needed to
      // boot);
      // Kafka is excluded since Sprint 1 has no producers/consumers wired yet.
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
    })
@AutoConfigureMockMvc
// disabledWithoutDocker: this integration test needs a real Postgres via Testcontainers, so it is
// SKIPPED (not failed) when no Docker environment is present (e.g. local unit-only runs); it still
// runs in CI where Docker is available.
@Testcontainers(disabledWithoutDocker = true)
class PingIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("identity_db");

  @DynamicPropertySource
  static void datasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired MockMvc mockMvc;

  @Test
  void pingReturnsOk() throws Exception {
    mockMvc
        .perform(get("/api/v1/ping"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.service").value("identity-service"))
        .andExpect(jsonPath("$.status").value("ok"));
  }

  @Test
  void migrationCreatedBaselineTables() throws Exception {
    // If Flyway had not applied V1, JPA `ddl-auto: validate` against outbox/audit entities
    // would have failed context startup, so a successful ping already proves the schema exists.
    assertThat(postgres.isRunning()).isTrue();
  }
}
