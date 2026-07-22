package com.serenmeet.auth.support;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import java.util.Set;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantMybatisConfig {

  private static final Set<String> GLOBAL_TABLES = Set.of(
    "tenant",
    "admin_user",
    "auth_session",
    "platform_config",
    "support_wechat",
    "audit_log",
    "idempotency_request"
  );

  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
      @Override
      public Expression getTenantId() {
        return new LongValue(TenantContext.requireTenantId());
      }

      @Override
      public boolean ignoreTable(String tableName) {
        return GLOBAL_TABLES.contains(tableName.toLowerCase());
      }
    }));
    return interceptor;
  }
}
