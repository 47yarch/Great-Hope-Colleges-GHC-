
package com.ghc.dao;

import com.ghc.database.DatabaseConnection;
import com.ghc.model.Student;
import com.ghc.security.AccessControl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
     * Create or update a student.
     * Only Administrator and Registrar may modify student records.
     */
    public void save(Student student) throws SQLException {
        AccessControl.requireAdminOrRegistrar();
        validateStudent(student);

        String sql;

        if (student.id == 0) {
            sql = """
                INSERT INTO STUDENTS (
                    STUDENT_NO, FIRST_NAME, LAST_NAME, GENDER,
                    BIRTH_DATE, EMAIL, PHONE, ADDRESS,
                    YEAR_LEVEL, PROGRAM, STATUS
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        } else {
            sql = """
                UPDATE STUDENTS SET
                    STUDENT_NO = ?, FIRST_NAME = ?, LAST_NAME = ?,
                    GENDER = ?, BIRTH_DATE = ?, EMAIL = ?, PHONE = ?,
                    ADDRESS = ?, YEAR_LEVEL = ?, PROGRAM = ?, STATUS = ?
                WHERE ID = ?
                """;
        }

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            int index = bindStudent(p, student);

            if (student.id != 0) {
                p.setInt(index, student.id);
            }

            int affected = p.executeUpdate();

            if (student.id != 0 && affected == 0) {
                throw new SQLException(
                        "Student not found. No record was updated.");
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
