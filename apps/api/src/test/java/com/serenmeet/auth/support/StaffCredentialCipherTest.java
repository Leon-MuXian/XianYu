package com.serenmeet.auth.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.serenmeet.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

class StaffCredentialCipherTest {

  private static final String OLD_KEY = encodedKey("0123456789abcdef0123456789abcdef");
  private static final String NEW_KEY = encodedKey("abcdef0123456789abcdef0123456789");

  @Test
  void decryptsOldCredentialAfterKeyRotationAndBindsCiphertextToStaff() {
    StaffCredentialCipher oldCipher =
      new StaffCredentialCipher("old", "old=" + OLD_KEY, "aes-256-gcm-v1");
    StaffCredentialCipher.EncryptedCredential encrypted =
      oldCipher.encrypt(10L, 20L, "staff-pass-2026");
    StaffCredentialCipher rotatedCipher = new StaffCredentialCipher(
      "new", "new=" + NEW_KEY + ",old=" + OLD_KEY, "aes-256-gcm-v1"
    );

    assertThat(rotatedCipher.decrypt(
      10L,
      20L,
      encrypted.keyId(),
      encrypted.cipherVersion(),
      encrypted.nonce(),
      encrypted.ciphertext()
    )).isEqualTo("staff-pass-2026");
    assertThatThrownBy(() -> rotatedCipher.decrypt(
      10L,
      21L,
      encrypted.keyId(),
      encrypted.cipherVersion(),
      encrypted.nonce(),
      encrypted.ciphertext()
    )).isInstanceOf(ApiException.class);
  }

  @Test
  void rejectsMissingMalformedAndUnavailableKeys() {
    assertThatThrownBy(() ->
      new StaffCredentialCipher("", "", "aes-256-gcm-v1")
    ).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() ->
      new StaffCredentialCipher("active", "active=dG9vLXNob3J0", "aes-256-gcm-v1")
    ).isInstanceOf(IllegalStateException.class);

    StaffCredentialCipher cipher =
      new StaffCredentialCipher("new", "new=" + NEW_KEY, "aes-256-gcm-v1");
    assertThatThrownBy(() -> cipher.requireAvailableKeys(List.of("old")))
      .isInstanceOf(IllegalStateException.class);
  }

  private static String encodedKey(String value) {
    return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }
}
