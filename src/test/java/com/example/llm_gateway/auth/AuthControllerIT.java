package com.example.llm_gateway.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
class AuthControllerIT {
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
  void signup_returnsTokens() {
    var resp = signup("alice@acme.test", "password1", "Acme");

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody()).containsKeys("accessToken", "refreshToken");
  }

  @Test
  void login_afterSignup_returnsTokens() {
    signup("bob@acme.test", "password1", "AcmeTwo");

    var resp =
        rest.postForEntity(
            url("/v1/auth/login"),
            Map.of("email", "bob@acme.test", "password", "password1"),
            Map.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(resp.getBody()).containsKeys("accessToken", "refreshToken");
  }

  @Test
  void login_wrongPassword_returns401() {
    signup("carol@acme.test", "rightpass", "AcmeThree");

    var resp =
        rest.postForEntity(
            url("/v1/auth/login"),
            Map.of("email", "carol@acme.test", "password", "wrongpass"),
            Map.class);
    System.out.println("STATUS: " + resp.getStatusCode());                                                                                                                                            
    System.out.println("BODY: " + resp.getBody());
    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void refresh_rotates_oldTokenNoLongerWorks() {
    var oldRefresh =
        (String) signup("dave@acme.test", "password1", "AcmeFour").getBody().get("refreshToken");

    var first =
        rest.postForEntity(url("/v1/auth/refresh"), Map.of("refreshToken", oldRefresh), Map.class);
    assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);

    var second =
        rest.postForEntity(url("/v1/auth/refresh"), Map.of("refreshToken", oldRefresh), Map.class);
    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  private ResponseEntity<Map> signup(String email, String password, String orgName) {
    return rest.postForEntity(
        url("/v1/auth/signup"),
        Map.of("orgName", orgName, "email", email, "password", password),
        Map.class);
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }
}
