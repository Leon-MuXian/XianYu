package com.serenmeet.store.dto;

import java.util.Map;

/**
 * 门店每周营业时间。
 */
public record BusinessHoursView(Map<String, DayHoursView> days) {
}
