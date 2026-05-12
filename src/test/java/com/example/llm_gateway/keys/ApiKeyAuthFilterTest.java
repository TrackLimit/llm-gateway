package com.example.llm_gateway.keys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.llm_gateway.auth.ApiKey;
import com.example.llm_gateway.auth.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthFilterTest {

  @Mock ApiKeyRepository keys;
  @InjectMocks ApiKeyAuthFilter filter;

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void validKey_setsApiKeyPrincipal() throws Exception {
    var raw = KeyGenerator.PREFIX + "validkey";
    var apiKey = mock(ApiKey.class);
    when(apiKey.getId()).thenReturn(42L);
    when(apiKey.getUserId()).thenReturn(7L);
    when(keys.findByKeyHashAndRevokedAtIsNull(KeyGenerator.hash(raw)))
        .thenReturn(Optional.of(apiKey));

    var req = request("POST", "/v1/completions", "Bearer " + raw);
    var chain = mock(FilterChain.class);
    filter.doFilter(req, new MockHttpServletResponse(), chain);

    var auth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth).isNotNull();
    var principal = (ApiKeyAuthFilter.ApiKeyPrincipal) auth.getPrincipal();
    assertThat(principal.apiKeyId()).isEqualTo(42L);
    assertThat(principal.userId()).isEqualTo(7L);
    verify(chain).doFilter(any(), any());
  }

  @Test
  void revokedKey_doesNotAuthenticate() throws Exception {
    var raw = KeyGenerator.PREFIX + "revoked";
    when(keys.findByKeyHashAndRevokedAtIsNull(KeyGenerator.hash(raw))).thenReturn(Optional.empty());

    var req = request("POST", "/v1/completions", "Bearer " + raw);
    var chain = mock(FilterChain.class);
    filter.doFilter(req, new MockHttpServletResponse(), chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(any(), any());
  }

  @Test
  void missingHeader_doesNotAuthenticate() throws Exception {
    var req = request("POST", "/v1/completions", null);
    var chain = mock(FilterChain.class);
    filter.doFilter(req, new MockHttpServletResponse(), chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(any(), any());
    verifyNoInteractions(keys);
  }

  @Test
  void wrongPrefix_doesNotAuthenticate() throws Exception {
    var req = request("POST", "/v1/completions", "Bearer eyJhbGc.notAnApiKey");
    var chain = mock(FilterChain.class);
    filter.doFilter(req, new MockHttpServletResponse(), chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(any(), any());
    verifyNoInteractions(keys);
  }

  @Test
  void nonCompletionsPath_skipsFilter() throws Exception {
    var req = request("GET", "/v1/keys", "Bearer " + KeyGenerator.PREFIX + "x");
    var chain = mock(FilterChain.class);
    filter.doFilter(req, new MockHttpServletResponse(), chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(any(), any());
    verifyNoInteractions(keys);
  }

  private MockHttpServletRequest request(String method, String uri, String authHeader) {
    var req = new MockHttpServletRequest(method, uri);
    if (authHeader != null) req.addHeader("Authorization", authHeader);
    return req;
  }
}
