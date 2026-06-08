package com.serenmeet.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 后台 API 的统一异常处理，避免前端吞掉字段错误和页面阻断。
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException exception) {
    return ResponseEntity.status(exception.status())
      .body(ApiResponse.fail(exception.code(), exception.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException exception) {
    String message = exception.getBindingResult().getFieldErrors().stream()
      .findFirst()
      .map(error -> error.getField() + "：" + error.getDefaultMessage())
      .orElse("表单字段不完整");
    return ResponseEntity.badRequest().body(ApiResponse.fail("FIELD_ERROR", message));
  }

  @ExceptionHandler({
    ConstraintViolationException.class,
    MissingServletRequestParameterException.class,
    HttpMessageNotReadableException.class,
    IllegalArgumentException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception exception) {
    return ResponseEntity.badRequest().body(ApiResponse.fail("BAD_REQUEST", "请求参数不正确"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
    log.error("Unhandled API exception", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(ApiResponse.fail("INTERNAL_ERROR", "服务暂时不可用，请稍后重试"));
  }
}
