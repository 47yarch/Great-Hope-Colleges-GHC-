
package com.ghc.dao;

import com.ghc.database.DatabaseConnection;
import com.ghc.model.Student;
import com.ghc.security.AccessControl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.ghc.database.DatabaseInitializer;
import java.util.Locale;

import javax.swing.JOptionPane;

public class StudentDAO {

    /**
     * Search students by student number, first name, last name, or program.
     * Any signed-in role may view and search student records.
     */
    public List<Student> all(String query) throws SQLException {
        AccessControl.requireSignedIn();

        List<Student> students = new ArrayList<>();

        String sql = """
            SELECT ID, STUDENT_NO, FIRST_NAME, LAST_NAME, GENDER,
                   BIRTH_DATE, EMAIL, PHONE, ADDRESS, YEAR_LEVEL,
                   PROGRAM, STATUS
            FROM STUDENTS
            WHERE LOWER(STUDENT_NO) LIKE ?
               OR LOWER(FIRST_NAME) LIKE ?
               OR LOWER(LAST_NAME) LIKE ?
               OR LOWER(PROGRAM) LIKE ?
            ORDER BY LAST_NAME, FIRST_NAME
            """;

        String value = "%" + normalize(query) + "%";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            for (int i = 1; i <= 4; i++) {
                p.setString(i, value);
            }

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    students.add(readStudent(r));
                }
            }
        }

        return students;
    }

    
/**
 * Compatibility method for existing callers.
 * New students must provide an initial password.
 */

public void save(Student student) throws SQLException {
    save(student, null);
}


/**
 * Creates or updates a student.
 * New students also receive a linked login account.
 */
public void save(Student student, String password) throws SQLException {
    AccessControl.requireAdminOrRegistrar();
    validateStudent(student);

    boolean isNew = student.id == 0;

    if (isNew && (password == null || password.length() < 8)) {
        throw new IllegalArgumentException(
                "The student's initial password must contain at least 8 characters.");
    }

    try (Connection c = DatabaseConnection.getConnection()) {
        c.setAutoCommit(false);

        try {
            int studentId;

            if (isNew) {
                String sql = """
                    INSERT INTO STUDENTS (
                        STUDENT_NO, FIRST_NAME, LAST_NAME, GENDER,
                        BIRTH_DATE, EMAIL, PHONE, ADDRESS,
                        YEAR_LEVEL, PROGRAM, STATUS
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

                try (PreparedStatement p = c.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {

                    bindStudent(p, student);
                    p.executeUpdate();

                    try (ResultSet keys = p.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "Could not retrieve the new student ID.");
                        }
                        studentId = keys.getInt(1);
                    }
                }

                String accountSql = """
                    INSERT INTO USERS (
                        USERNAME, PASSWORD_HASH, FULL_NAME,
                        ROLE, ACTIVE, STUDENT_ID
                    ) VALUES (?, ?, ?, 'STUDENT', 1, ?)
                    """;

                try (PreparedStatement p = c.prepareStatement(accountSql)) {
                    p.setString(1, student.studentNo.trim());
                    p.setString(2, DatabaseInitializer.hash(password));
                    p.setString(3,
                            student.firstName.trim() + " "
                            + student.lastName.trim());
                    p.setInt(4, studentId);
                    p.executeUpdate();
                }

                // Keep the generated ID on the model.
                student.id = studentId;

            } else {
                String sql = """
                    UPDATE STUDENTS SET
                        STUDENT_NO = ?, FIRST_NAME = ?, LAST_NAME = ?,
                        GENDER = ?, BIRTH_DATE = ?, EMAIL = ?, PHONE = ?,
                        ADDRESS = ?, YEAR_LEVEL = ?, PROGRAM = ?, STATUS = ?
                    WHERE ID = ?
                    """;

                try (PreparedStatement p = c.prepareStatement(sql)) {
                    int index = bindStudent(p, student);
                    p.setInt(index, student.id);

                    if (p.executeUpdate() == 0) {
                        throw new SQLException(
                                "Student not found. No record was updated.");
                    }
                
                

if (password != null && !password.isBlank()) {
    if (password.length() < 8) {
        throw new IllegalArgumentException(
                "The new password must contain at least 8 characters.");
    }

    // First, look for an existing account linked by student ID.
    String findAccountSql = """
        SELECT ID
        FROM USERS
        WHERE STUDENT_ID = ? AND ROLE = 'STUDENT'
        """;

    Integer userId = null;

    try (PreparedStatement findAccount =
            c.prepareStatement(findAccountSql)) {
        findAccount.setInt(1, student.id);

        try (ResultSet result = findAccount.executeQuery()) {
            if (result.next()) {
                userId = result.getInt("ID");
            }
        }
    }

    if (userId != null) {
        // Update the existing linked account.
        String updateAccountSql = """
            UPDATE USERS
            SET PASSWORD_HASH = ?
            WHERE ID = ?
            """;

        try (PreparedStatement updateAccount =
                c.prepareStatement(updateAccountSql)) {
            updateAccount.setString(
                    1, DatabaseInitializer.hash(password));
            updateAccount.setInt(2, userId);
            updateAccount.executeUpdate();
        }
    } else {
        // No linked account exists, so create one.
        String createAccountSql = """
            INSERT INTO USERS (
                USERNAME, PASSWORD_HASH, FULL_NAME,
                ROLE, ACTIVE, STUDENT_ID
            ) VALUES (?, ?, ?, 'STUDENT', 1, ?)
            """;

        try (PreparedStatement createAccount =
                c.prepareStatement(createAccountSql)) {
            createAccount.setString(1, student.studentNo.trim());
            createAccount.setString(
                    2, DatabaseInitializer.hash(password));
            createAccount.setString(
                    3, student.firstName.trim() + " "
                    + student.lastName.trim());
            createAccount.setInt(4, student.id);
            createAccount.executeUpdate();
        }
    }
}


                }
            }

            c.commit();

        } catch (Exception ex) {
            try {
                c.rollback();
            } catch (SQLException rollbackError) {
                ex.addSuppressed(rollbackError);
            }

            if (ex instanceof SQLException sqlException) {
                throw sqlException;
            }
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new SQLException("Could not save student and login account.", ex);

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
     * Delete a student. Database foreign-key constraints may prevent
     * deletion when enrollment records refer to that student.
     */
    public void delete(int id) throws SQLException {
        AccessControl.requireAdminOrRegistrar();

        if (id <= 0) {
            throw new IllegalArgumentException("Invalid student ID.");
        }

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p =
                     c.prepareStatement("DELETE FROM STUDENTS WHERE ID = ?")) {

            p.setInt(1, id);

            if (p.executeUpdate() == 0) {
                throw new SQLException("Student not found.");
            }
        }
    }

    public int count() throws SQLException {
        AccessControl.requireSignedIn();
        return countTable("STUDENTS");
    }

    /**
     * Only fixed table names are accepted to prevent SQL injection.
     */
    public static int countTable(String table) throws SQLException {
        AccessControl.requireSignedIn();

        String safeTable = switch (table == null ? "" : table.toUpperCase()) {
            case "STUDENTS", "COURSES", "ENROLLMENTS", "USERS" -> table.toUpperCase();
            default -> throw new IllegalArgumentException(
                    "Unsupported table name.");
        };

        String sql = "SELECT COUNT(*) FROM " + safeTable;

        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) {

            r.next();
            return r.getInt(1);
        }
    }

    private static Student readStudent(ResultSet r) throws SQLException {
        return new Student(
                r.getInt("ID"),
                r.getString("STUDENT_NO"),
                r.getString("FIRST_NAME"),
                r.getString("LAST_NAME"),
                r.getString("GENDER"),
                r.getString("BIRTH_DATE"),
                r.getString("EMAIL"),
                r.getString("PHONE"),
                r.getString("ADDRESS"),
                r.getString("YEAR_LEVEL"),
                r.getString("PROGRAM"),
                r.getString("STATUS")
        );
    }

    private static int bindStudent(
            PreparedStatement p, Student student) throws SQLException {

        int i = 1;

        p.setString(i++, student.studentNo.trim());
        p.setString(i++, student.firstName.trim());
        p.setString(i++, student.lastName.trim());
        p.setString(i++, nullable(student.gender));
        p.setString(i++, nullable(student.birthDate));
        p.setString(i++, nullable(student.email));
        p.setString(i++, nullable(student.phone));
        p.setString(i++, nullable(student.address));
        p.setString(i++, nullable(student.yearLevel));
        p.setString(i++, nullable(student.program));
        p.setString(i++, normalizeStatus(student.status));

        return i;
    }

    private static void validateStudent(Student s) {
        if (s == null) {
            throw new IllegalArgumentException("Student is required.");
        }

        if (blank(s.studentNo)
                || blank(s.firstName)
                || blank(s.lastName)) {
            throw new IllegalArgumentException(
                    "Student number, first name, and last name are required.");
        }

        if (s.studentNo.trim().length() > 30) {
            throw new IllegalArgumentException(
                    "Student number cannot exceed 30 characters.");
        }

        if (s.firstName.trim().length() > 80
                || s.lastName.trim().length() > 80) {
            throw new IllegalArgumentException(
                    "First name and last name cannot exceed 80 characters.");
        }

        if (!blank(s.email)
                && (s.email.length() > 120
                || !s.email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
    }

    private static String normalizeStatus(String status) {
        return blank(status) ? "Active" : status.trim();
    }

    private static String nullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
