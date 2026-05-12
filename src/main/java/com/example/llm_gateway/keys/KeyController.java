package com.example.llm_gateway.keys;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/keys")
@RequiredArgsConstructor
public class KeyController {

  private final KeyService keys;

  @PostMapping
  public KeyService.CreatedKey create(
      @AuthenticationPrincipal Long userId, @Valid @RequestBody CreateKeyRequest req) {
    return keys.create(userId, req.name());
  }

  @GetMapping
  public List<KeyService.KeySummary> list(@AuthenticationPrincipal Long userId) {
    return keys.list(userId);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revoke(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
    keys.revoke(userId, id);
  }

  public record CreateKeyRequest(@NotBlank @Size(max = 255) String name) {}
}
