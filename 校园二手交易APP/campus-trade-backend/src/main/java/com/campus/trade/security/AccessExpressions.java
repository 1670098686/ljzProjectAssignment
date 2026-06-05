package com.campus.trade.security;

/**
 * Central place to hold SpEL expressions reused across @PreAuthorize annotations.
 */
public final class AccessExpressions {

    private AccessExpressions() {
    }

    public static final String MEMBER = "isAuthenticated() and hasAnyAuthority('ROLE_STUDENT','ROLE_TEACHER','ROLE_STAFF')";
    public static final String ADMIN = "hasAuthority('ROLE_ADMIN')";
}
