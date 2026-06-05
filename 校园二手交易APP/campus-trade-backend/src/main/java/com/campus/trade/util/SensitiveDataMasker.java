package com.campus.trade.util;

public final class SensitiveDataMasker {

    private SensitiveDataMasker() {
    }

    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex == email.length() - 1) {
            return maskMiddle(email, 1, 1);
        }
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***" + domainPart;
        }
        String maskedLocal = localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1);
        return maskedLocal + domainPart;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        if (phone.length() <= 4) {
            return maskMiddle(phone, 1, 1);
        }
        int prefixLength = Math.min(3, phone.length() - 2);
        int suffixLength = Math.min(2, phone.length() - prefixLength);
        return maskMiddle(phone, prefixLength, suffixLength);
    }

    private static String maskMiddle(String value, int prefixLength, int suffixLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= prefixLength + suffixLength) {
            return value.charAt(0) + "***";
        }
        String prefix = value.substring(0, prefixLength);
        String suffix = value.substring(value.length() - suffixLength);
        return prefix + "***" + suffix;
    }
}
