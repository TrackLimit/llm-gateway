package com.example.llm_gateway.keys;

import com.example.llm_gateway.auth.ApiKey;
import com.example.llm_gateway.auth.ApiKeyRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class KeyService {

  private final ApiKeyRepository keys;

  @Transactional
  public CreatedKey create(Long userId, String name) {
    var generated = KeyGenerator.generate();
    var saved = keys.save(new ApiKey(userId, generated.hash(), generated.prefix(), name));
    return new CreatedKey(saved.getId(), generated.raw(), generated.prefix(), name);
  }

  public List<KeySummary> list(Long userId) {
    return keys.findByUserIdAndRevokedAtIsNull(userId).stream()
        .map(k -> new KeySummary(k.getId(), k.getKeyPrefix(), k.getName(), k.getCreatedAt()))
        .toList();
  }

  @Transactional
  public void revoke(Long userId, Long keyId) {
    var key =
        keys.findById(keyId)
            .filter(k -> k.getUserId().equals(userId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    key.revoke();
  }

  public record CreatedKey(Long id, String rawKey, String prefix, String name) {}

  public record KeySummary(Long id, String prefix, String name, Instant createdAt) {}
}
