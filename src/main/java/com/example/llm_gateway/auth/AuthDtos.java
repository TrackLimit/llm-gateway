package com.example.llm_gateway.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
  public record SignupRequest(
      @NotBlank @Size(max = 255) String orgName,
      @Email @NotBlank String email,
      @NotBlank @Size(min = 8, max = 72) String password) {}

  public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

  public record RefreshRequest(@NotBlank String refreshToken) {}

  public record TokenResponse(String accessToken, String refreshToken) {}
}
