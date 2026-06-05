package com.campus.trade.model.enums;

import java.util.Optional;

public enum AdminPermission {
    PRODUCT_REVIEW,
    USER_MANAGEMENT,
    MESSAGE_REVIEW,
    NOTIFICATION_MANAGEMENT,
    ADMIN_MANAGEMENT,
    STATISTICS_VIEW;

    private static final String AUTHORITY_PREFIX = "ADMIN_";

    public String getAuthority() {
        return AUTHORITY_PREFIX + name();
    }

    public static Optional<AdminPermission> fromName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(AdminPermission.valueOf(name.trim().toUpperCase()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
