package com.serenmeet.auth.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serenmeet.common.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RealWechatCodeSessionClientTest {

  private MockRestServiceServer server;
  private RealWechatCodeSessionClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    client = new RealWechatCodeSessionClient(
      builder,
      new ObjectMapper(),
      "owner-app-id",
      "owner-secret",
      "member-app-id",
      "member-secret"
    );
  }

  @Test
  void acceptsJsonBodyReturnedAsTextPlain() {
    server.expect(anything()).andRespond(withSuccess(
      "{\"openid\":\"owner-open-id\",\"session_key\":\"session-key\"}",
      MediaType.TEXT_PLAIN
    ));

    String openId = client.exchange("owner", "wechat-code");

    assertThat(openId).isEqualTo("owner-open-id");
    server.verify();
  }

  @Test
  void rejectsWechatErrorReturnedAsTextPlain() {
    server.expect(anything()).andRespond(withSuccess(
      "{\"errcode\":40029,\"errmsg\":\"invalid code\"}",
      MediaType.TEXT_PLAIN
    ));

    assertThatThrownBy(() -> client.exchange("owner", "invalid-code"))
      .isInstanceOfSatisfying(ApiException.class, exception -> {
        assertThat(exception.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exception.code()).isEqualTo("WECHAT_LOGIN_FAILED");
        assertThat(exception).hasMessage("微信登录凭证无效");
      });
    server.verify();
  }

  @Test
  void rejectsMalformedWechatResponse() {
    server.expect(anything()).andRespond(withSuccess("not-json", MediaType.TEXT_PLAIN));

    assertThatThrownBy(() -> client.exchange("owner", "wechat-code"))
      .isInstanceOfSatisfying(ApiException.class, exception -> {
        assertThat(exception.status()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(exception.code()).isEqualTo("WECHAT_LOGIN_FAILED");
        assertThat(exception).hasMessage("微信登录暂时不可用");
      });
    server.verify();
  }
}
