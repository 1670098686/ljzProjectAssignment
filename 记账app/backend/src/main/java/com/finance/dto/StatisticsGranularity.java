package com.finance.dto;

import com.finance.exception.BusinessException;

public enum StatisticsGranularity {
    DAILY,
    WEEKLY,
    MONTHLY,
    QUARTERLY;

    public static StatisticsGranularity from(String value) {
        if (value == null || value.isBlank()) {
            return DAILY;
        }
        try {
            return StatisticsGranularity.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("granularity must be one of daily, weekly, monthly, quarterly");
        }
    }
}
