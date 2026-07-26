package com.serenmeet.common;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    exception.getBindingResult().getFieldErrors().forEach(error ->
      fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
    );
    return ResponseEntity.badRequest().body(ApiResponse.fieldError("请检查表单内容", fieldErrors));
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

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(NoResourceFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
      .body(ApiResponse.fail("RESOURCE_NOT_FOUND", "请求资源不存在"));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException exception) {
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
      .body(ApiResponse.fail("METHOD_NOT_ALLOWED", "当前接口不支持该请求方式"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
    log.error("Unhandled API exception", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(ApiResponse.fail("INTERNAL_ERROR", "服务暂时不可用，请稍后重试"));
  }
}
