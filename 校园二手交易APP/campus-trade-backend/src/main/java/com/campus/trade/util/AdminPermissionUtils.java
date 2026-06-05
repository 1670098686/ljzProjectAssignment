package com.campus.trade.util;

import com.campus.trade.model.entity.Admin;
import com.campus.trade.model.enums.AdminPermission;
import com.campus.trade.model.enums.AdminRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class AdminPermissionUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AdminPermissionUtils() {
    }

    public static EnumSet<AdminPermission> normalizePermissions(AdminRole role,
                                                                Collection<AdminPermission> requested) {
        if (role == AdminRole.SUPER_ADMIN) {
            return EnumSet.allOf(AdminPermission.class);
        }
        if (requested == null || requested.isEmpty()) {
            return defaultPermissions(role);
        }
        EnumSet<AdminPermission> cleaned = EnumSet.noneOf(AdminPermission.class);
        requested.stream()
                .filter(Objects::nonNull)
                .forEach(cleaned::add);
        if (cleaned.isEmpty()) {
            return defaultPermissions(role);
        }
        return cleaned;
    }

    public static EnumSet<AdminPermission> defaultPermissions(AdminRole role) {
        if (role == AdminRole.MODERATOR) {
            return EnumSet.of(AdminPermission.PRODUCT_REVIEW, AdminPermission.MESSAGE_REVIEW);
        }
        if (role == AdminRole.ADMIN) {
            return EnumSet.of(
                    AdminPermission.PRODUCT_REVIEW,
                    AdminPermission.USER_MANAGEMENT,
                    AdminPermission.MESSAGE_REVIEW,
                    AdminPermission.NOTIFICATION_MANAGEMENT
            );
        }
        return EnumSet.allOf(AdminPermission.class);
    }

    public static EnumSet<AdminPermission> resolveStoredPermissions(Admin admin) {
        EnumSet<AdminPermission> stored = parsePermissions(admin.getPermissions());
        if (stored.isEmpty()) {
            return defaultPermissions(admin.getRole());
        }
        if (admin.getRole() == AdminRole.SUPER_ADMIN) {
            stored.addAll(EnumSet.allOf(AdminPermission.class));
        }
        return stored;
    }

    public static String serializePermissions(Collection<AdminPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return null;
        }
        List<String> names = permissions.stream()
                .map(AdminPermission::name)
                .collect(Collectors.toList());
        try {
            return OBJECT_MAPPER.writeValueAsString(names);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize admin permissions", e);
        }
    }

    public static List<String> toNameList(Collection<AdminPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(permissions.stream()
                .map(AdminPermission::name)
                .toList());
    }

    private static EnumSet<AdminPermission> parsePermissions(String raw) {
        EnumSet<AdminPermission> result = EnumSet.noneOf(AdminPermission.class);
        if (!StringUtils.hasText(raw)) {
            return result;
        }
        List<String> values = tryParseJsonList(raw.trim());
        if (values.isEmpty()) {
            values = splitFallback(raw);
        }
        values.stream()
                .map(AdminPermission::fromName)
                .flatMap(Optional::stream)
                .forEach(result::add);
        return result;
    }

    private static List<String> tryParseJsonList(String raw) {
        if (!raw.startsWith("[") || !raw.endsWith("]")) {
            return List.of();
        }
        try {
            return OBJECT_MAPPER.readValue(raw, new TypeReference<List<String>>() { });
        } catch (IOException e) {
            return List.of();
        }
    }

    private static List<String> splitFallback(String raw) {
        String[] parts = raw.split(",");
        List<String> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                result.add(part.trim().replace("\"", ""));
            }
        }
        return result;
    }
}
