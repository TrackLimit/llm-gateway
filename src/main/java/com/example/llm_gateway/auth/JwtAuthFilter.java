package com.example.llm_gateway.auth;

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
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwt;

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    var header = req.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      try {
        var claims = jwt.parse(header.substring(7));
        var userId = Long.valueOf(claims.getSubject());
        var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
      } catch (Exception ignored) {
        // invalid token - fall through, request will be rejected by authorization rules
      }
    }
    chain.doFilter(req, res);
  }
}
