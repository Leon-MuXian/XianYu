package com.serenmeet.common;

import java.util.List;

/**
 * 后台分页响应。
 */
public record PageResponse<T>(List<T> items, long total, int page, int pageSize) {
}
