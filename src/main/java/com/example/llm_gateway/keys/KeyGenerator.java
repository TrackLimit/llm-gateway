package com.example.llm_gateway.keys;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class KeyGenerator {

  private static final SecureRandom RNG = new SecureRandom();
  public static final String PREFIX = "gw_";

  public static GeneratedKey generate() {
    var bytes = new byte[32];
    RNG.nextBytes(bytes);
    var raw = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    return new GeneratedKey(raw, raw.substring(0, 11), hash(raw));
  }

  public static String hash(String raw) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes()));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public record GeneratedKey(String raw, String prefix, String hash) {}

  private KeyGenerator() {}
}
