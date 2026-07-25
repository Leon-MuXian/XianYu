package com.serenmeet.auth.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.serenmeet.common.ApiException;
import com.serenmeet.auth.support.StaffCredentialCipher;
import com.serenmeet.owner.mapper.OwnerPilotMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StaffCredentialKeyStartupValidatorTest {

  @Test
  void rejectsWrongKeyMaterialBeforeApplicationBecomesReady() {
    StaffCredentialCipher sourceCipher = new StaffCredentialCipher(
      "shared", "shared=" + encodedKey("0123456789abcdef0123456789abcdef"),
      "aes-256-gcm-v1"
    );
    StaffCredentialCipher.EncryptedCredential encrypted =
      sourceCipher.encrypt(10L, 20L, "staff-pass-2026");
    StaffCredentialCipher wrongCipher = new StaffCredentialCipher(
      "shared", "shared=" + encodedKey("abcdef0123456789abcdef0123456789"),
      "aes-256-gcm-v1"
    );
    OwnerPilotMapper mapper = mock(OwnerPilotMapper.class);
    when(mapper.selectStaffCredentialKeyIds()).thenReturn(List.of("shared"));
    when(mapper.selectStaffCredentialValidationSamples()).thenReturn(List.of(Map.of(
      "tenantId", 10L,
      "staffId", 20L,
      "keyId", encrypted.keyId(),
      "cipherVersion", encrypted.cipherVersion(),
      "nonce", encrypted.nonce(),
      "ciphertext", encrypted.ciphertext()
    )));
    StaffCredentialKeyStartupValidator validator =
      new StaffCredentialKeyStartupValidator(mapper, wrongCipher);

    assertThatThrownBy(() -> validator.run(null)).isInstanceOf(ApiException.class);
  }

  private static String encodedKey(String value) {
    return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }
}
