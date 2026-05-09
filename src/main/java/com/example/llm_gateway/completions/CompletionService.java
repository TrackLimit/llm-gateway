package com.example.llm_gateway.completions;

import com.example.llm_gateway.auth.UsageLog;
import com.example.llm_gateway.auth.UsageLogRepository;
import com.example.llm_gateway.completions.CompletionDtos.CompletionRequest;
import com.example.llm_gateway.completions.CompletionDtos.CompletionResponse;
import com.example.llm_gateway.providers.LlmProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CompletionService {

  private final List<LlmProvider> providers;
  private final UsageLogRepository usageLogs;

  public CompletionResponse complete(Long apiKeyId, CompletionRequest req) {
    var provider =
        providers.stream()
            .filter(p -> p.supports(req.model()))
            .findFirst()
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Unsupported model: " + req.model()));

    var started = System.nanoTime();
    var result =
        provider.complete(
            new LlmProvider.CompletionInput(
                req.model(), req.prompt(), req.maxTokens(), req.temperature()));
    var latencyMs = (System.nanoTime() - started) / 1_000_000;

    usageLogs.save(
        new UsageLog(
            apiKeyId,
            provider.name(),
            req.model(),
            result.promptTokens(),
            result.completionTokens(),
            (int) latencyMs));

    return new CompletionResponse(
        result.text(),
        provider.name(),
        req.model(),
        result.promptTokens(),
        result.completionTokens(),
        latencyMs);
  }
}
