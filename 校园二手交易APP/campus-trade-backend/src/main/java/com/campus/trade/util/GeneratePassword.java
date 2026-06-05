package com.campus.trade.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "123456";
        String encryptedPassword = encoder.encode(password);
        System.out.println("Password: " + password);
        System.out.println("Encrypted password: " + encryptedPassword);
        
        // Test the match
        boolean matches = encoder.matches(password, encryptedPassword);
        System.out.println("Password match: " + matches);
    }
}