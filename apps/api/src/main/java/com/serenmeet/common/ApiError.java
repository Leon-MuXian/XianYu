package com.serenmeet.common;

/**
 * API 错误信息，面向前端展示页面级或表单级错误。
 */
public record ApiError(String code, String message) {
}
