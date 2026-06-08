package com.serenmeet.common;

/**
 * 后台分页参数规范化结果。
 */
public record PageQuery(int page, int pageSize) {

  private static final int MAX_PAGE_SIZE = 50;

  /**
   * 规范化分页参数，避免负数 offset 或超大 pageSize。
   */
  public static PageQuery normalize(int page, int pageSize) {
    int safePage = Math.max(1, page);
    int safePageSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
    return new PageQuery(safePage, safePageSize);
  }

  public int offset() {
    return (page - 1) * pageSize;
  }
}
