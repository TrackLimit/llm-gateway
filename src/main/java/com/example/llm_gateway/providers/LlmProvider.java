package com.example.llm_gateway.providers;

public interface LlmProvider {
  String name();

  boolean supports(String model);

  CompletionResult complete(CompletionInput input);

  record CompletionInput(String model, String prompt, Integer maxTokens, Double temperature) {}

  record CompletionResult(String text, int promptTokens, int completionTokens) {}
}
