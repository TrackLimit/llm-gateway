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
@Table(name = "usage_logs")
@Getter
@NoArgsConstructor
public class UsageLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "api_key_id", nullable = false)
  private Long apiKeyId;

  @Column(nullable = false)
  private String provider;

  @Column(nullable = false)
  private String model;

  @Column(name = "prompt_tokens", nullable = false)
  private Integer promptTokens;

  @Column(name = "completion_tokens", nullable = false)
  private Integer completionTokens;

  @Column(name = "latency_ms", nullable = false)
  private Integer latencyMs;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  public UsageLog(
      Long apiKeyId,
      String provider,
      String model,
      Integer promptTokens,
      Integer completionTokens,
      Integer latencyMs) {
    this.apiKeyId = apiKeyId;
    this.provider = provider;
    this.model = model;
    this.promptTokens = promptTokens;
    this.completionTokens = completionTokens;
    this.latencyMs = latencyMs;
  }
}
