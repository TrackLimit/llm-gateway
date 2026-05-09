package com.example.llm_gateway.completions;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CompletionDtos {

  public record CompletionRequest(
      @NotBlank @Size(max = 8000) String prompt,
      @NotBlank String model,
      @Min(1) @Max(4096) Integer maxTokens,
      @DecimalMin("0.0") @DecimalMax("2.0") Double temperature) {}

  public record CompletionResponse(
      String text,
      String provider,
      String model,
      int promptTokens,
      int completionTokens,
      long latencyMs) {}
}
