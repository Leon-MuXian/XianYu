package com.serenmeet.auth.support;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 后台接口鉴权和当前用户注入配置。
 */
@Configuration
public class AdminWebMvcConfig implements WebMvcConfigurer {

  private final AdminAuthInterceptor authInterceptor;
  private final AdminUserArgumentResolver adminUserArgumentResolver;

  public AdminWebMvcConfig(AdminAuthInterceptor authInterceptor, AdminUserArgumentResolver adminUserArgumentResolver) {
    this.authInterceptor = authInterceptor;
    this.adminUserArgumentResolver = adminUserArgumentResolver;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(authInterceptor)
      .addPathPatterns("/admin/**")
      .excludePathPatterns("/admin/auth/login");
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(adminUserArgumentResolver);
  }
}
