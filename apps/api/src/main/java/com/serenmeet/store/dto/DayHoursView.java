package com.serenmeet.store.dto;

/**
 * 单个星期日的营业时间。
 */
public record DayHoursView(boolean open, String start, String end) {
}
