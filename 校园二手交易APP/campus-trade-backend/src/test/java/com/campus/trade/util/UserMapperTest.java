package com.campus.trade.util;

import com.campus.trade.dto.user.UserSummary;
import com.campus.trade.model.entity.User;
import com.campus.trade.model.enums.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserMapperTest {

    @Test
    void toMaskedSummary_shouldHideEmailAndPhone() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPhone("13800138000");
        user.setRole(UserRole.STUDENT);

        UserSummary masked = UserMapper.toMaskedSummary(user);

        assertEquals("a***e@example.com", masked.getEmail());
        assertEquals("138***00", masked.getPhone());
    }

    @Test
    void toSummary_shouldKeepSensitiveFields() {
        User user = new User();
        user.setId(2L);
        user.setUsername("bob");
        user.setEmail("bob@example.com");
        user.setPhone("15500001111");
        user.setRole(UserRole.STUDENT);

        UserSummary summary = UserMapper.toSummary(user);

        assertEquals("bob@example.com", summary.getEmail());
        assertEquals("15500001111", summary.getPhone());
    }
}
