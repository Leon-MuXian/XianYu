package com.serenmeet.auth.support;

import com.serenmeet.common.ApiException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 后台密码哈希校验，种子账号也不在数据库存明文。
 */
@Component
public class PasswordHasher {

  /**
   * 校验 PBKDF2-SHA256 密码。
   */
  public boolean matches(String rawPassword, String encoded) {
    try {
      String[] parts = encoded.split("\\$");
      if (parts.length != 4 || !"pbkdf2_sha256".equals(parts[0])) {
        throw invalidHash();
      }
      int iterations = Integer.parseInt(parts[1]);
      byte[] salt = HexFormat.of().parseHex(parts[2]);
      byte[] expected = HexFormat.of().parseHex(parts[3]);
      byte[] actual = pbkdf2(rawPassword, salt, iterations, expected.length);
      return constantTimeEquals(actual, expected);
    } catch (IllegalArgumentException exception) {
      throw invalidHash();
    }
  }

  private ApiException invalidHash() {
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PASSWORD_HASH_INVALID", "后台账号密码配置异常");
  }

  private byte[] pbkdf2(String rawPassword, byte[] salt, int iterations, int length) {
    try {
      KeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, length * 8);
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "PASSWORD_HASH_FAILED", "后台账号密码校验失败");
    }
  }

  private boolean constantTimeEquals(byte[] left, byte[] right) {
    if (left.length != right.length) {
      return false;
    }
    int result = 0;
    for (int i = 0; i < left.length; i += 1) {
      result |= left[i] ^ right[i];
    }
    return result == 0;
  }
}
