package com.finance.dto;

import com.finance.exception.BusinessException;

public enum SortDirection {
    ASC,
    DESC;

    public static SortDirection from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return SortDirection.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("sort must be 'asc' or 'desc'");
        }
    }
}
