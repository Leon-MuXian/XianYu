package com.serenmeet.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;

class ApiExceptionHandlerTest {

  @Test
  void mapsUnsupportedMethodToMethodNotAllowedInsteadOfInternalServerError() {
    ApiExceptionHandler handler = new ApiExceptionHandler();
    HttpRequestMethodNotSupportedException exception =
        new HttpRequestMethodNotSupportedException("DELETE", List.of("GET"));

    var response = handler.handleMethodNotSupported(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().success()).isFalse();
    assertThat(response.getBody().error().code()).isEqualTo("METHOD_NOT_ALLOWED");
  }
}
