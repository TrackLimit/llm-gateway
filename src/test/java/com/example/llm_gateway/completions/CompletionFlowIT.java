package com.example.llm_gateway.completions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.llm_gateway.providers.LlmProvider.CompletionResult;
import com.example.llm_gateway.providers.impl.GeminiProvider;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CompletionFlowIT {

  private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
      new ParameterizedTypeReference<>() {};

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine3.22");

  @Container
  static GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:8")).withExposedPorts(6379);

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    registry.add("gateway.providers.gemini.api-key", () -> "test-key");
  }

  @LocalServerPort int port;
  @Autowired TestRestTemplate rest;
  @MockitoBean GeminiProvider gemini;

  @BeforeEach
  void stubProvider() {
    when(gemini.name()).thenReturn("gemini");
    when(gemini.supports("gemini-2.0-flash")).thenReturn(true);
    when(gemini.complete(any())).thenReturn(new CompletionResult("hello world", 5, 7));
  }

  @Test
  void completion_withApiKey_returnsModelOutput() {
    var jwt = signupAndGetToken("alice@acme.test");
    var rawKey = (String) post("/v1/keys", jwt, Map.of("name", "ci-key")).getBody().get("rawKey");

    var resp =
        post(
            "/v1/completions",
            rawKey,
            Map.of(
                "prompt",
                "hello",
                "model",
                "gemini-2.0-flash",
                "maxTokens",
                32,
                "temperature",
                0.7));

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    var body = resp.getBody();
    assertThat(body.get("text")).isEqualTo("hello world");
    assertThat(body.get("provider")).isEqualTo("gemini");
    assertThat(body.get("model")).isEqualTo("gemini-2.0-flash");
    assertThat(body.get("promptTokens")).isEqualTo(5);
    assertThat(body.get("completionTokens")).isEqualTo(7);
  }

  @Test
  void completion_withoutKey_returns401() {
    var resp =
        rest.exchange(
            url("/v1/completions"),
            HttpMethod.POST,
            new HttpEntity<>(
                Map.of(
                    "prompt",
                    "hello",
                    "model",
                    "gemini-2.0-flash",
                    "maxTokens",
                    32,
                    "temperature",
                    0.7)),
            MAP_TYPE);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  private String signupAndGetToken(String email) {
    var resp =
        rest.exchange(
            url("/v1/auth/signup"),
            HttpMethod.POST,
            new HttpEntity<>(Map.of("orgName", "Acme", "email", email, "password", "password1")),
            MAP_TYPE);
    return (String) resp.getBody().get("accessToken");
  }

  private ResponseEntity<Map<String, Object>> post(
      String path, String token, Map<String, Object> body) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return rest.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, headers), MAP_TYPE);
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }
}
