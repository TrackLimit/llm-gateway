package com.example.llm_gateway.keys;

import com.example.llm_gateway.auth.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

  private final ApiKeyRepository keys;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest req) {
    return !req.getRequestURI().startsWith("/v1/completions");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    var header = req.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      chain.doFilter(req, res);
      return;
    }
    var raw = header.substring(7);
    if (!raw.startsWith(KeyGenerator.PREFIX)) {
      chain.doFilter(req, res);
      return;
    }
    keys.findByKeyHashAndRevokedAtIsNull(KeyGenerator.hash(raw))
        .ifPresent(
            key -> {
              var auth =
                  new UsernamePasswordAuthenticationToken(
                      new ApiKeyPrincipal(key.getId(), key.getUserId()), null, List.of());
              SecurityContextHolder.getContext().setAuthentication(auth);
            });
    chain.doFilter(req, res);
  }

  public record ApiKeyPrincipal(Long apiKeyId, Long userId) {}
}
