package com.example.llm_gateway.keys;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class KeyControllerIT {

  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine3.22");

  @Container
  static GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:8")).withExposedPorts(6379);

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }

  @LocalServerPort int port;
  @Autowired TestRestTemplate rest;

  @Test
  void create_returnsRawKeyAndPrefix() {
    var token = signupAndGetToken("alice@acme.test");

    var resp = post("/v1/keys", token, Map.of("name", "ci-key"));

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody()).containsKeys("id", "rawKey", "prefix", "name");
    assertThat((String) resp.getBody().get("rawKey")).startsWith("gw_");
    assertThat((String) resp.getBody().get("prefix")).startsWith("gw_");
  }

  @Test
  void list_doesNotIncludeRawKey() {
    var token = signupAndGetToken("bob@acme.test");
    post("/v1/keys", token, Map.of("name", "ci-key"));

    var resp = get("/v1/keys", token);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<Map<String, Object>> body = resp.getBody();
    assertThat(body).hasSize(1);
    assertThat(body.get(0)).containsKeys("id", "prefix", "name", "createdAt");
    assertThat(body.get(0)).doesNotContainKey("rawKey");
  }

  @Test
  void revoke_removesFromList() {
    var token = signupAndGetToken("carol@acme.test");
    var created = post("/v1/keys", token, Map.of("name", "ci-key")).getBody();
    var id = ((Number) created.get("id")).longValue();

    var deleteResp = exchange("/v1/keys/" + id, HttpMethod.DELETE, token, null);
    assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

    var listResp = get("/v1/keys", token);
    assertThat((List<?>) listResp.getBody()).isEmpty();
  }

  @Test
  void unauthenticated_returns401() {
    var resp = rest.postForEntity(url("/v1/keys"), Map.of("name", "x"), Map.class);
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  private String signupAndGetToken(String email) {
    var resp =
        rest.postForEntity(
            url("/v1/auth/signup"),
            Map.of("orgName", "Acme", "email", email, "password", "password1"),
            Map.class);
    return (String) resp.getBody().get("accessToken");
  }

  private ResponseEntity<Map<String, Object>> post(
      String path, String token, Map<String, Object> body) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return rest.exchange(
        url(path),
        HttpMethod.POST,
        new HttpEntity<>(body, headers),
        new ParameterizedTypeReference<Map<String, Object>>() {});
  }

  private ResponseEntity<List<Map<String, Object>>> get(String path, String token) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return rest.exchange(
        url(path),
        HttpMethod.GET,
        new HttpEntity<>(headers),
        new ParameterizedTypeReference<List<Map<String, Object>>>() {});
  }

  private ResponseEntity<Void> exchange(String path, HttpMethod method, String token, Object body) {
    var headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return rest.exchange(url(path), method, new HttpEntity<>(body, headers), Void.class);
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }
}
