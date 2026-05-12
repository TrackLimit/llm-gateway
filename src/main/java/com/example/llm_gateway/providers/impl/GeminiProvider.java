package com.example.llm_gateway.providers.impl;

import com.example.llm_gateway.providers.LlmProvider;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GeminiProvider implements LlmProvider {

  private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
  private final RestClient client;
  private final String apiKey;

  public GeminiProvider(@Value("${gateway.providers.gemini.api-key}") String apiKey) {
    this.apiKey = apiKey;

    var requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofSeconds(5));
    requestFactory.setReadTimeout(Duration.ofSeconds(60));

    this.client = RestClient.builder().baseUrl(BASE_URL).requestFactory(requestFactory).build();
  }

  @Override
  public String name() {
    return "gemini";
  }

  @Override
  public boolean supports(String model) {
    return model.startsWith("gemini-");
  }

  @Override
  public CompletionResult complete(CompletionInput input) {
    var body =
        Map.of(
            "contents",
            List.of(Map.of("parts", List.of(Map.of("text", input.prompt())))),
            "generationConfig",
            Map.of("maxOutputTokens", input.maxTokens(), "temperature", input.temperature()));

    var resp =
        client
            .post()
            .uri("/models/{model}:generateContent?key={key}", input.model(), apiKey)
            .body(body)
            .retrieve()
            .body(GeminiResponse.class);

    if (resp == null || resp.candidates() == null || resp.candidates().isEmpty()) {
      throw new IllegalStateException("Gemini returned no candidates (likely safety-blocked)");
    }
    var content = resp.candidates().get(0).content();
    if (content == null || content.parts() == null || content.parts().isEmpty()) {
      throw new IllegalStateException("Gemini candidate has no content parts");
    }
    var usage = resp.usageMetadata();
    if (usage == null) {
      throw new IllegalStateException("Gemini response missing usage metadata");
    }

    var text = content.parts().get(0).text();
    return new CompletionResult(text, usage.promptTokenCount(), usage.candidatesTokenCount());
  }

  record GeminiResponse(List<Candidate> candidates, UsageMetadata usageMetadata) {}

  record Candidate(Content content) {}

  record Content(List<Part> parts) {}

  record Part(String text) {}

  record UsageMetadata(int promptTokenCount, int candidatesTokenCount) {}
}
