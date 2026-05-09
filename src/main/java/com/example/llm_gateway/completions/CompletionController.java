package com.example.llm_gateway.completions;

import com.example.llm_gateway.completions.CompletionDtos.CompletionRequest;
import com.example.llm_gateway.completions.CompletionDtos.CompletionResponse;
import com.example.llm_gateway.keys.ApiKeyAuthFilter.ApiKeyPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/completions")
@RequiredArgsConstructor
public class CompletionController {

  private final CompletionService completions;

  @PostMapping
  public CompletionResponse complete(
      @AuthenticationPrincipal ApiKeyPrincipal principal,
      @Valid @RequestBody CompletionRequest req) {
    return completions.complete(principal.apiKeyId(), req);
  }
}
