package com.serenmeet.common;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * 闲遇四端 HTTP 契约入口。
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "闲遇 API", version = "mvp", description = "店长、员工、会员和平台后台统一 API"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "opaque")
public class OpenApiConfig {
}
