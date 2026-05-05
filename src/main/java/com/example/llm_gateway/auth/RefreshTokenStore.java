package com.example.llm_gateway.auth;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenStore {
  private static final SecureRandom RNG = new SecureRandom();
  private final StringRedisTemplate redis;
  private final Duration ttl;

  public RefreshTokenStore(
      StringRedisTemplate redis, @Value("${gateway.jwt.refresh-ttl}") Duration ttl) {
    this.redis = redis;
    this.ttl = ttl;
  }

  public String issue(Long userId) {
    var raw = UUID.randomUUID() + "-" + Long.toHexString(RNG.nextLong());
    redis.opsForValue().set(key(raw), userId.toString(), ttl);
    return raw;
  }

  public Optional<Long> consume(String raw) {
    var k = key(raw);
    var userId = redis.opsForValue().getAndDelete(k);
    return Optional.ofNullable(userId).map(Long::parseLong);
  }

  private String key(String rawToken) {
    try {
      var digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes());
      return "refresh:" + HexFormat.of().formatHex(digest);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
