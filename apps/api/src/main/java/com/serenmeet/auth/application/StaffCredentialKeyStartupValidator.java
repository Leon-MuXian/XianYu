package com.serenmeet.auth.application;

import com.serenmeet.auth.support.StaffCredentialCipher;
import com.serenmeet.owner.mapper.OwnerPilotMapper;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 服务启动时阻止缺少历史员工凭据密钥的错误配置进入可用状态。 */
@Component
public class StaffCredentialKeyStartupValidator implements ApplicationRunner {

  private final OwnerPilotMapper ownerMapper;
  private final StaffCredentialCipher credentialCipher;

  public StaffCredentialKeyStartupValidator(
      OwnerPilotMapper ownerMapper, StaffCredentialCipher credentialCipher) {
    this.ownerMapper = ownerMapper;
    this.credentialCipher = credentialCipher;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    credentialCipher.requireAvailableKeys(ownerMapper.selectStaffCredentialKeyIds());
    for (Map<String, Object> sample : ownerMapper.selectStaffCredentialValidationSamples()) {
      credentialCipher.decrypt(
          asLong(sample.get("tenantId")),
          asLong(sample.get("staffId")),
          sample.get("keyId").toString(),
          sample.get("cipherVersion").toString(),
          (byte[]) sample.get("nonce"),
          (byte[]) sample.get("ciphertext"));
    }
  }

  private Long asLong(Object value) {
    return ((Number) value).longValue();
  }
}
