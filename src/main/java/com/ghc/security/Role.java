package com.ghc.security;

import java.util.Locale;

public enum Role {
    ADMIN,
    REGISTRAR,
    INSTRUCTOR,
    STUDENT;

    public static Role fromDatabase(String value) {
        if (value == null) {
            throw new IllegalArgumentException("User role is missing.");
        }

        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "ADMIN", "ADMINISTRATOR" -> ADMIN;
            case "REGISTRAR" -> REGISTRAR;
            case "INSTRUCTOR" -> INSTRUCTOR;
            case "STUDENT" -> STUDENT;
            default -> throw new IllegalArgumentException(
                    "Unsupported user role: " + value);
        };
    }
}