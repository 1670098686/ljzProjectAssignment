package com.campus.trade.util;

import com.campus.trade.dto.user.UserSummary;
import com.campus.trade.model.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserSummary toSummary(User user) {
        return toSummary(user, false);
    }

    public static UserSummary toMaskedSummary(User user) {
        return toSummary(user, true);
    }

    private static UserSummary toSummary(User user, boolean maskSensitive) {
        if (user == null) {
            return null;
        }
        String email = maskSensitive ? SensitiveDataMasker.maskEmail(user.getEmail()) : user.getEmail();
        String phone = maskSensitive ? SensitiveDataMasker.maskPhone(user.getPhone()) : user.getPhone();
        return new UserSummary(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                email,
                phone,
                user.getAvatar(),
                user.getSchool(),
                user.isEmailVerified()
        );
    }
}
