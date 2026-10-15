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

public class UserDAO {

    /**
     * Authenticates a user and returns the user's ID, name, and role.
     * Returns null for invalid credentials or inactive accounts.
     */
    public UserSession login(String username, String password)
            throws Exception {

        if (blank(username) || password == null || password.isEmpty()) {
            return null;
        }

        String sql = """
            SELECT ID, USERNAME, PASSWORD_HASH, FULL_NAME, ROLE, ACTIVE
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

                Role role = Role.fromDatabase(r.getString("ROLE"));

                return new UserSession(
                        r.getInt("ID"),
                        r.getString("USERNAME"),
                        r.getString("FULL_NAME"),
                        role
                );
            }
        }
    }

    /**
     * Compatibility method for code that still expects boolean authentication.
     */
    public boolean authenticate(String username, String password)
            throws Exception {
        return login(username, password) != null;
    }

    /**
     * Returns account information without returning password hashes.
     * Only Administrators can list accounts.
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
                            r.getInt("ACTIVE") == 1 ? "Active" : "Inactive"
                    });
                }
            }
        }

        return users;
    }

    /**
     * Creates a new user account.
     * The password is stored as a SHA-256 hash for compatibility with
     * the existing application. See security note below.
     */
    public void createUser(
            String username,
            String password,
            String fullName,
            Role role) throws Exception {

        AccessControl.requireAdmin();

        validateAccount(username, password, fullName, role);

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
     * Changes a user's display name and role.
     * Does not change the password or account activation state.
     */
    public void updateUser(
            int userId,
            String fullName,
            Role role) throws SQLException {

        AccessControl.requireAdmin();

        if (userId <= 0 || blank(fullName) || role == null) {
            throw new IllegalArgumentException(
                    "A valid user ID, full name, and role are required.");
        }

        if (fullName.trim().length() > 120) {
            throw new IllegalArgumentException(
                    "Full name cannot exceed 120 characters.");
        }

        preventChangingOwnRole(userId, role);

        String sql = "UPDATE USERS SET FULL_NAME = ?, ROLE = ? WHERE ID = ?";

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
     * Prevents an Administrator from deactivating their own account.
     */
    public void setActive(int userId, boolean active) throws SQLException {
        AccessControl.requireAdmin();

        if (userId <= 0) {
            throw new IllegalArgumentException("Invalid user ID.");
        }

        UserSession current = AccessControl.requireAdmin();

        if (current.getUserId() == userId && !active) {
            throw new SecurityException(
                    "You cannot deactivate your own signed-in account.");
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
     * A signed-in Administrator must provide a new password.
     */
    public void resetPassword(int userId, String newPassword)
            throws Exception {

        AccessControl.requireAdmin();

        if (userId <= 0 || newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException(
                    "The user ID must be valid and the password must contain " +
                    "at least 8 characters.");
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
                    "Username, password, full name, and role are required.");
        }

        if (username.trim().length() > 50) {
            throw new IllegalArgumentException(
                    "Username cannot exceed 50 characters.");
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters.");
        }

        if (fullName.trim().length() > 120) {
            throw new IllegalArgumentException(
                    "Full name cannot exceed 120 characters.");
        }
    }

    private static void preventChangingOwnRole(
            int targetUserId, Role newRole) {

        UserSession current = AccessControl.requireAdmin();

        if (current.getUserId() == targetUserId
                && current.getRole() != newRole) {
            throw new SecurityException(
                    "You cannot change your own role while signed in.");
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
