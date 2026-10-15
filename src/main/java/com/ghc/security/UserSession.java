package com.ghc.security;

public final class UserSession {
    private final int userId;
    private final String username;
    private final String fullName;
    private final Role role;

    public UserSession(
            int userId,
            String username,
            String fullName,
            Role role) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isRegistrar() {
        return role == Role.REGISTRAR;
    }

    public boolean isInstructor() {
        return role == Role.INSTRUCTOR;
    }
}
