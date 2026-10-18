
package com.ghc.security;

public final class UserSession {

    private final int userId;
    private final String username;
    private final String fullName;
    private final Role role;
    private final Integer studentId;

    public UserSession(
            int userId,
            String username,
            String fullName,
            Role role) {
        this(userId, username, fullName, role, null);
    }

    public UserSession(
            int userId,
            String username,
            String fullName,
            Role role,
            Integer studentId) {

        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.studentId = studentId;
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

    public Integer getStudentId() {
        return studentId;
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

    public boolean isStudent() {
        return role == Role.STUDENT;
    }
}
