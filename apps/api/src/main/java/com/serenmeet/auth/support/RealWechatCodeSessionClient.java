package com.serenmeet.auth.support;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serenmeet.common.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Profile("!local & !test")
@Slf4j
public class RealWechatCodeSessionClient implements WechatCodeSessionClient {

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String ownerAppId;
  private final String ownerSecret;
  private final String memberAppId;
  private final String memberSecret;

  public RealWechatCodeSessionClient(
      RestClient.Builder restClientBuilder,
      ObjectMapper objectMapper,
      @Value("${seren-meet.wechat.owner.app-id:}") String ownerAppId,
      @Value("${seren-meet.wechat.owner.secret:}") String ownerSecret,
      @Value("${seren-meet.wechat.member.app-id:}") String memberAppId,
      @Value("${seren-meet.wechat.member.secret:}") String memberSecret) {
    this.restClient = restClientBuilder.baseUrl("https://api.weixin.qq.com").build();
    this.objectMapper = objectMapper;
    this.ownerAppId = ownerAppId;
    this.ownerSecret = ownerSecret;
    this.memberAppId = memberAppId;
    this.memberSecret = memberSecret;
  }

  @Override
  public String exchange(String application, String code) {
    String appId = "owner".equals(application) ? ownerAppId : memberAppId;
    String secret = "owner".equals(application) ? ownerSecret : memberSecret;
    if (appId.isBlank() || secret.isBlank()) {
      throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WECHAT_NOT_CONFIGURED", "微信登录尚未配置");
    }
    String responseBody;
    try {
      responseBody = restClient.get()
        .uri(uriBuilder -> uriBuilder.path("/sns/jscode2session")
          .queryParam("appid", appId)
          .queryParam("secret", secret)
          .queryParam("js_code", code)
          .queryParam("grant_type", "authorization_code")
          .build())
        .retrieve()
        .body(String.class);
    } catch (RestClientException exception) {
      log.warn(
        "WeChat code2session request failed, exceptionType={}, rootCauseType={}",
        exception.getClass().getName(),
        rootCauseType(exception)
      );
      throw unavailable(exception);
    }

    if (responseBody == null || responseBody.isBlank()) {
      log.warn("WeChat code2session returned an empty response");
      throw unavailable(null);
    }

    CodeSessionResponse response;
    try {
      response = objectMapper.readValue(responseBody, CodeSessionResponse.class);
    } catch (JsonProcessingException exception) {
      log.warn(
        "WeChat code2session returned an unreadable response, exceptionType={}",
        exception.getClass().getName()
      );
      throw unavailable(exception);
    }
    boolean invalidResponse = response.openid() == null
      || response.openid().isBlank()
      || response.errorCode() != null;
    if (invalidResponse) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "WECHAT_LOGIN_FAILED", "微信登录凭证无效");
    }
    return response.openid();
  }

  private static ApiException unavailable(Throwable cause) {
    if (cause == null) {
      return new ApiException(HttpStatus.BAD_GATEWAY, "WECHAT_LOGIN_FAILED", "微信登录暂时不可用");
    }
    return new ApiException(
      HttpStatus.BAD_GATEWAY,
      "WECHAT_LOGIN_FAILED",
      "微信登录暂时不可用",
      cause
    );
  }

  private static String rootCauseType(Throwable throwable) {
    Throwable cause = throwable;
    while (cause.getCause() != null && cause.getCause() != cause) {
      cause = cause.getCause();
    }
    return cause.getClass().getName();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record CodeSessionResponse(String openid, @JsonProperty("errcode") Integer errorCode) {
  }
}
