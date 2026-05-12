package com.example.llm_gateway.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "api_keys")
@Getter
@NoArgsConstructor
public class ApiKey {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "key_hash", nullable = false, unique = true)
  private String keyHash;

  @Column(name = "key_prefix", nullable = false, length = 16)
  private String keyPrefix;

  @Column(nullable = false)
  private String name;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "revoked_at")
  private Instant revokedAt;

  public ApiKey(Long userId, String keyHash, String keyPrefix, String name) {
    this.userId = userId;
    this.keyHash = keyHash;
    this.keyPrefix = keyPrefix;
    this.name = name;
  }

  public void revoke() {
    if (this.revokedAt == null) {
      this.revokedAt = Instant.now();
    }
  }

  public boolean isActive() {
    return revokedAt == null;
  }
}
