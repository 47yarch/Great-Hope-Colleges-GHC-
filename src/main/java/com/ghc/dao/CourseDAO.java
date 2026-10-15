package com.ghc.dao;

import com.ghc.database.DatabaseConnection;
import com.ghc.model.Course;
import com.ghc.security.AccessControl;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseDAO {

    public List<Course> all(String query) throws SQLException {
        AccessControl.requireSignedIn();

        List<Course> courses = new ArrayList<>();

        String sql = """
            SELECT ID, COURSE_CODE, COURSE_NAME, UNITS, DESCRIPTION
            FROM COURSES
            WHERE LOWER(COURSE_CODE) LIKE ?
               OR LOWER(COURSE_NAME) LIKE ?
            ORDER BY COURSE_CODE
            """;

        String value = "%" + normalize(query) + "%";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, value);
            p.setString(2, value);

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    courses.add(new Course(
                            r.getInt("ID"),
                            r.getString("COURSE_CODE"),
                            r.getString("COURSE_NAME"),
                            r.getInt("UNITS"),
                            r.getString("DESCRIPTION")
                    ));
                }
            }
        }

        return courses;
    }

    /**
     * Create or update a course.
     * Only Administrators may change academic course definitions.
     */
    public void save(Course course) throws SQLException {
        AccessControl.requireAdmin();
        validate(course);

        String sql;

        if (course.id == 0) {
            sql = """
                INSERT INTO COURSES
                    (COURSE_CODE, COURSE_NAME, UNITS, DESCRIPTION)
                VALUES (?, ?, ?, ?)
                """;
        } else {
            sql = """
                UPDATE COURSES
                SET COURSE_CODE = ?, COURSE_NAME = ?,
                    UNITS = ?, DESCRIPTION = ?
                WHERE ID = ?
                """;
        }

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, course.code.trim());
            p.setString(2, course.name.trim());
            p.setInt(3, course.units);
            p.setString(4, nullable(course.description));

            if (course.id != 0) {
                p.setInt(5, course.id);
            }

            int affected = p.executeUpdate();

            if (course.id != 0 && affected == 0) {
                throw new SQLException(
                        "Course not found. No record was updated.");
            }
        }
    }

    /**
     * Deleting a course referenced by enrollments is prevented by
     * the database foreign-key constraint.
     */
    public void delete(int id) throws SQLException {
        AccessControl.requireAdmin();

        if (id <= 0) {
            throw new IllegalArgumentException("Invalid course ID.");
        }

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p =
                     c.prepareStatement("DELETE FROM COURSES WHERE ID = ?")) {

            p.setInt(1, id);

            if (p.executeUpdate() == 0) {
                throw new SQLException("Course not found.");
            }
        }
    }

    public int count() throws SQLException {
        AccessControl.requireSignedIn();

        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM COURSES")) {

            r.next();
            return r.getInt(1);
        }
    }

    private static void validate(Course course) {
        if (course == null) {
            throw new IllegalArgumentException("Course is required.");
        }

        if (blank(course.code) || blank(course.name)) {
            throw new IllegalArgumentException(
                    "Course code and course name are required.");
        }

        if (course.code.trim().length() > 30) {
            throw new IllegalArgumentException(
                    "Course code cannot exceed 30 characters.");
        }

        if (course.name.trim().length() > 120) {
            throw new IllegalArgumentException(
                    "Course name cannot exceed 120 characters.");
        }

        if (course.units <= 0 || course.units > 30) {
            throw new IllegalArgumentException(
                    "Course units must be between 1 and 30.");
        }

        if (course.description != null
                && course.description.length() > 250) {
            throw new IllegalArgumentException(
                    "Description cannot exceed 250 characters.");
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static String nullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
