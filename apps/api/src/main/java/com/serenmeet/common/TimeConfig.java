package com.serenmeet.common;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 业务时间配置，统一后端日期判断和测试注入。
 */
@Configuration
public class TimeConfig {

  @Bean
  public Clock businessClock() {
    return Clock.system(ZoneId.of("Asia/Shanghai"));
  }
}
