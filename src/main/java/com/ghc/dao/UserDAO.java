
package com.ghc.dao;

import com.ghc.database.DatabaseConnection;
import com.ghc.database.DatabaseInitializer;
import com.ghc.security.AccessControl;
import com.ghc.security.Role;
import com.ghc.security.UserSession;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserDAO {

    /**
     * Authenticates a user and returns the user's session.
     * Returns null for invalid credentials or inactive accounts.
     */
    public UserSession login(String username, String password)
            throws Exception {

        if (blank(username) || password == null || password.isEmpty()) {
            return null;
        }

        String sql = """
                SELECT ID, USERNAME, PASSWORD_HASH, FULL_NAME,
                       ROLE, ACTIVE, STUDENT_ID
                FROM USERS
                WHERE USERNAME = ?
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, username.trim());

            try (ResultSet r = p.executeQuery()) {
                if (!r.next() || r.getInt("ACTIVE") != 1) {
                    return null;
                }

                String storedHash = r.getString("PASSWORD_HASH");
                String enteredHash = DatabaseInitializer.hash(password);

                if (storedHash == null
                        || !MessageDigest.isEqual(
                                storedHash.getBytes(StandardCharsets.UTF_8),
                                enteredHash.getBytes(StandardCharsets.UTF_8))) {
                    return null;
                }

                int id = r.getInt("ID");
                String accountUsername = r.getString("USERNAME");
                String fullName = r.getString("FULL_NAME");
                Role role = Role.fromDatabase(r.getString("ROLE"));

                // Preserve SQL NULL for accounts not linked to a student.
                int studentIdValue = r.getInt("STUDENT_ID");
Integer studentId = null;

if (!r.wasNull()) {
    studentId = Integer.valueOf(studentIdValue);
}

                if (role == Role.STUDENT && studentId == null) {
                    throw new IllegalStateException(
                            "This student account is not linked to a student "
                            + "record. Ask the Administrator to link it."
                    );
                }

                return new UserSession(
                        id,
                        accountUsername,
                        fullName,
                        role,
                        studentId
                );
            }
        }
    }

    /**
     * Compatibility method for existing code.
     */
    public boolean authenticate(String username, String password)
            throws Exception {
        return login(username, password) != null;
    }

    /**
     * Lists accounts. Administrators only.
     */
    public List<Object[]> all(String query) throws SQLException {
        AccessControl.requireAdmin();

        List<Object[]> users = new ArrayList<>();

        String sql = """
                SELECT ID, USERNAME, FULL_NAME, ROLE, ACTIVE
                FROM USERS
                WHERE LOWER(USERNAME) LIKE ?
                   OR LOWER(FULL_NAME) LIKE ?
                   OR LOWER(ROLE) LIKE ?
                ORDER BY USERNAME
                """;

        String value = "%" + normalize(query) + "%";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, value);
            p.setString(2, value);
            p.setString(3, value);

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    users.add(new Object[]{
                            r.getInt("ID"),
                            r.getString("USERNAME"),
                            r.getString("FULL_NAME"),
                            r.getString("ROLE"),
                            r.getInt("ACTIVE") == 1
                                    ? "Active" : "Inactive"
                    });
                }
            }
        }

        return users;
    }

    /**
     * Creates an Administrator, Registrar, or Instructor account.
     * Student accounts must use createStudentUser().
     */
    public void createUser(
            String username,
            String password,
            String fullName,
            Role role) throws Exception {

        AccessControl.requireAdmin();
        validateAccount(username, password, fullName, role);

        if (role == Role.STUDENT) {
            throw new IllegalArgumentException(
                    "Create student accounts through createStudentUser() "
                    + "to link them to a student record."
            );
        }

        String sql = """
                INSERT INTO USERS
                    (USERNAME, PASSWORD_HASH, FULL_NAME, ROLE, ACTIVE)
                VALUES (?, ?, ?, ?, 1)
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, username.trim());
            p.setString(2, DatabaseInitializer.hash(password));
            p.setString(3, fullName.trim());
            p.setString(4, role.name());

            p.executeUpdate();
        }
    }

    /**
     * Creates a student account linked to an existing student record.
     */
    public void createStudentUser(
            String username,
            String password,
            int studentId) throws Exception {

        AccessControl.requireAdmin();

        if (blank(username) || username.trim().length() > 50) {
            throw new IllegalArgumentException(
                    "Enter a username of at most 50 characters."
            );
        }

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters."
            );
        }

        if (studentId <= 0) {
            throw new IllegalArgumentException(
                    "Select a valid student record."
            );
        }

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);

            try {
                String fullName;

                try (PreparedStatement p = c.prepareStatement("""
                        SELECT FIRST_NAME, LAST_NAME
                        FROM STUDENTS
                        WHERE ID = ?
                          AND UPPER(COALESCE(STATUS, 'ACTIVE')) = 'ACTIVE'
                        """)) {

                    p.setInt(1, studentId);

                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) {
                            throw new SQLException(
                                    "Active student record not found."
                            );
                        }

                        fullName = r.getString("FIRST_NAME")
                                + " " + r.getString("LAST_NAME");
                    }
                }

                try (PreparedStatement p = c.prepareStatement(
                        "SELECT ID FROM USERS WHERE STUDENT_ID = ?")) {

                    p.setInt(1, studentId);

                    try (ResultSet r = p.executeQuery()) {
                        if (r.next()) {
                            throw new SQLException(
                                    "This student already has a login account."
                            );
                        }
                    }
                }

                try (PreparedStatement p = c.prepareStatement("""
                        INSERT INTO USERS
                            (USERNAME, PASSWORD_HASH, FULL_NAME,
                             ROLE, ACTIVE, STUDENT_ID)
                        VALUES (?, ?, ?, 'STUDENT', 1, ?)
                        """)) {

                    p.setString(1, username.trim());
                    p.setString(2, DatabaseInitializer.hash(password));
                    p.setString(3, fullName);
                    p.setInt(4, studentId);

                    p.executeUpdate();
                }

                c.commit();

            } catch (Exception ex) {
                try {
                    c.rollback();
                } catch (SQLException rollbackError) {
                    ex.addSuppressed(rollbackError);
                }

                throw ex;

            } finally {
                try {
                    c.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // Connection closes after this method.
                }
            }
        }
    }

    /**
     * Updates a user's display name and role.
     */
    public void updateUser(
            int userId,
            String fullName,
            Role role) throws SQLException {

        AccessControl.requireAdmin();

        if (userId <= 0 || blank(fullName) || role == null) {
            throw new IllegalArgumentException(
                    "A valid user ID, full name, and role are required."
            );
        }

        if (fullName.trim().length() > 120) {
            throw new IllegalArgumentException(
                    "Full name cannot exceed 120 characters."
            );
        }

        if (role == Role.STUDENT) {
            throw new IllegalArgumentException(
                    "Use student account creation to assign the STUDENT role."
            );
        }

        preventChangingOwnRole(userId, role);

        String sql = """
                UPDATE USERS
                SET FULL_NAME = ?, ROLE = ?
                WHERE ID = ?
                """;

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, fullName.trim());
            p.setString(2, role.name());
            p.setInt(3, userId);

            if (p.executeUpdate() == 0) {
                throw new SQLException("User account not found.");
            }
        }
    }

    /**
     * Enables or disables an account.
     */
    public void setActive(int userId, boolean active) throws SQLException {
        AccessControl.requireAdmin();

        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID.");
        }

        UserSession current = AccessControl.requireAdmin();

        if (current.getUserId() == userId && !active) {
            throw new SecurityException(
                    "You cannot deactivate your own signed-in account."
            );
        }

        String sql = "UPDATE USERS SET ACTIVE = ? WHERE ID = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, active ? 1 : 0);
            p.setInt(2, userId);

            if (p.executeUpdate() == 0) {
                throw new SQLException("User account not found.");
            }
        }
    }

    /**
     * Resets another user's password.
     */
    public void resetPassword(int userId, String newPassword)
            throws Exception {

        AccessControl.requireAdmin();

        if (userId <= 0 || newPassword == null
                || newPassword.length() < 8) {
            throw new IllegalArgumentException(
                    "The user ID must be valid and the password must contain "
                    + "at least 8 characters."
            );
        }

        String sql = "UPDATE USERS SET PASSWORD_HASH = ? WHERE ID = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, DatabaseInitializer.hash(newPassword));
            p.setInt(2, userId);

            if (p.executeUpdate() == 0) {
                throw new SQLException("User account not found.");
            }
        }
    }

    private static void validateAccount(
            String username,
            String password,
            String fullName,
            Role role) {

        if (blank(username) || blank(password)
                || blank(fullName) || role == null) {
            throw new IllegalArgumentException(
                    "Username, password, full name, and role are required."
            );
        }

        if (username.trim().length() > 50) {
            throw new IllegalArgumentException(
                    "Username cannot exceed 50 characters."
            );
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters."
            );
        }

        if (fullName.trim().length() > 120) {
            throw new IllegalArgumentException(
                    "Full name cannot exceed 120 characters."
            );
        }
    }

    private static void preventChangingOwnRole(
            int targetUserId,
            Role newRole) {

        UserSession current = AccessControl.requireAdmin();

        if (current.getUserId() == targetUserId
                && current.getRole() != newRole) {
            throw new SecurityException(
                    "You cannot change your own role while signed in."
            );
        }
    }

    private static String normalize(String value) {
        return value == null
                ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
