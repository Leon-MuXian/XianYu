package com.serenmeet.auth.support;

import com.serenmeet.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** 使用可轮换 AES-256-GCM keyring 保护店长可恢复的员工登录密码。 */
@Component
public class StaffCredentialCipher {

  private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
  private static final String SUPPORTED_VERSION = "aes-256-gcm-v1";
  private static final int AES_KEY_BYTES = 32;
  private static final int NONCE_BYTES = 12;
  private static final int GCM_TAG_BITS = 128;
  private static final Pattern KEY_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,32}");

  private final SecureRandom secureRandom = new SecureRandom();
  private final String activeKeyId;
  private final String cipherVersion;
  private final Map<String, SecretKeySpec> keysById;

  public StaffCredentialCipher(
      @Value("${seren-meet.staff-credential.active-key-id:}") String activeKeyId,
      @Value("${seren-meet.staff-credential.keys:}") String configuredKeys,
      @Value("${seren-meet.staff-credential.cipher-version:aes-256-gcm-v1}")
          String cipherVersion) {
    this.activeKeyId = requireKeyId(activeKeyId);
    this.cipherVersion = requireSupportedVersion(cipherVersion);
    this.keysById = Map.copyOf(parseKeys(configuredKeys));
    if (!keysById.containsKey(this.activeKeyId)) {
      throw invalidConfiguration("Active staff credential key is missing from the keyring");
    }
  }

  public EncryptedCredential encrypt(Long tenantId, Long staffId, String rawPassword) {
    byte[] nonce = new byte[NONCE_BYTES];
    secureRandom.nextBytes(nonce);
    try {
      Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
      cipher.init(
          Cipher.ENCRYPT_MODE,
          keysById.get(activeKeyId),
          new GCMParameterSpec(GCM_TAG_BITS, nonce));
      cipher.updateAAD(aad(tenantId, staffId, activeKeyId, cipherVersion));
      byte[] ciphertext = cipher.doFinal(rawPassword.getBytes(StandardCharsets.UTF_8));
      return new EncryptedCredential(activeKeyId, cipherVersion, nonce, ciphertext);
    } catch (GeneralSecurityException exception) {
      throw credentialFailure(exception);
    }
  }

  public String decrypt(
      Long tenantId,
      Long staffId,
      String keyId,
      String storedCipherVersion,
      byte[] nonce,
      byte[] ciphertext) {
    SecretKeySpec key = keysById.get(keyId);
    if (key == null
        || !SUPPORTED_VERSION.equals(storedCipherVersion)
        || nonce == null
        || nonce.length != NONCE_BYTES
        || ciphertext == null) {
      throw credentialFailure(null);
    }
    try {
      Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, nonce));
      cipher.updateAAD(aad(tenantId, staffId, keyId, storedCipherVersion));
      return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException exception) {
      throw credentialFailure(exception);
    }
  }

  /** 启动阶段校验数据库正在使用的历史密钥仍存在，防止错误轮换后带病启动。 */
  public void requireAvailableKeys(Collection<String> requiredKeyIds) {
    for (String keyId : requiredKeyIds) {
      if (!keysById.containsKey(keyId)) {
        throw invalidConfiguration(
            "Staff credential keyring does not contain every key required by stored credentials");
      }
    }
  }

  private Map<String, SecretKeySpec> parseKeys(String configuredKeys) {
    if (configuredKeys == null || configuredKeys.isBlank()) {
      throw invalidConfiguration("Staff credential encryption keys are required");
    }
    Map<String, SecretKeySpec> parsed = new LinkedHashMap<>();
    for (String configuredEntry : configuredKeys.split(",")) {
      String entry = configuredEntry.trim();
      int separator = entry.indexOf('=');
      if (separator <= 0 || separator == entry.length() - 1) {
        throw invalidConfiguration("Staff credential keyring entries must use keyId=Base64 format");
      }
      String keyId = requireKeyId(entry.substring(0, separator));
      byte[] keyBytes;
      try {
        keyBytes = Base64.getDecoder().decode(entry.substring(separator + 1));
      } catch (IllegalArgumentException exception) {
        throw invalidConfiguration("Staff credential encryption key must use Base64");
      }
      if (keyBytes.length != AES_KEY_BYTES) {
        throw invalidConfiguration("Staff credential encryption key must contain 32 bytes");
      }
      if (parsed.putIfAbsent(keyId, new SecretKeySpec(keyBytes, "AES")) != null) {
        throw invalidConfiguration("Staff credential keyring contains duplicate key IDs");
      }
    }
    return parsed;
  }

  private String requireKeyId(String keyId) {
    String normalized = keyId == null ? "" : keyId.trim();
    if (!KEY_ID_PATTERN.matcher(normalized).matches()) {
      throw invalidConfiguration("Staff credential key ID is missing or invalid");
    }
    return normalized;
  }

  private String requireSupportedVersion(String configuredVersion) {
    if (!SUPPORTED_VERSION.equals(configuredVersion)) {
      throw invalidConfiguration("Unsupported staff credential cipher version");
    }
    return configuredVersion;
  }

  private byte[] aad(Long tenantId, Long staffId, String keyId, String version) {
    String value =
        "staff-credential|" + version + "|" + keyId + "|" + tenantId + "|" + staffId;
    return value.getBytes(StandardCharsets.UTF_8);
  }

  private ApiException credentialFailure(Throwable cause) {
    return new ApiException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "STAFF_CREDENTIAL_UNAVAILABLE",
        "员工登录凭据暂时不可用，请检查服务端密钥配置",
        cause);
  }

  private IllegalStateException invalidConfiguration(String message) {
    return new IllegalStateException(message);
  }

  public record EncryptedCredential(
      String keyId, String cipherVersion, byte[] nonce, byte[] ciphertext) {}
}
