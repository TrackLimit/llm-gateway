package com.example.llm_gateway.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;
  private final Duration accessTtl;

  public JwtService(
      @Value("${gateway.jwt.secret}") String secret,
      @Value("${gateway.jwt.access-ttl}") Duration accessTtl) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTtl = accessTtl;
  }

  public String issueAccessToken(Long userId, Long orgId) {
    var now = Instant.now();
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("orgId", orgId)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(accessTtl)))
        .signWith(key)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
