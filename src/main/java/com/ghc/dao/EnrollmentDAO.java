
package com.ghc.dao;

import com.ghc.database.DatabaseConnection;
import com.ghc.security.AccessControl;
import com.ghc.security.UserSession;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EnrollmentDAO {

    /**
     * Row layout retained for compatibility with the current UI:
     * 0 ID
     * 1 Student number
     * 2 Student full name
     * 3 Course code and name
     * 4 School year
     * 5 Semester
     * 6 Grade
     * 7 Remarks
     * 8 Student ID
     * 9 Course ID
     */
    public List<Object[]> all(String query) throws SQLException {
        AccessControl.requireSignedIn();

        List<Object[]> rows = new ArrayList<>();

        String sql = """
            SELECT E.ID,
                   S.STUDENT_NO,
                   S.FIRST_NAME,
                   S.LAST_NAME,
                   C.COURSE_CODE,
                   C.COURSE_NAME,
                   E.SCHOOL_YEAR,
                   E.SEMESTER,
                   E.GRADE,
                   E.REMARKS,
                   E.STUDENT_ID,
                   E.COURSE_ID
            FROM ENROLLMENTS E
            JOIN STUDENTS S ON E.STUDENT_ID = S.ID
            JOIN COURSES C ON E.COURSE_ID = C.ID
            WHERE LOWER(S.STUDENT_NO) LIKE ?
               OR LOWER(S.FIRST_NAME) LIKE ?
               OR LOWER(S.LAST_NAME) LIKE ?
               OR LOWER(C.COURSE_CODE) LIKE ?
            ORDER BY E.SCHOOL_YEAR DESC, S.LAST_NAME, S.FIRST_NAME
            """;

        String value = "%" + normalize(query) + "%";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            for (int i = 1; i <= 4; i++) {
                p.setString(i, value);
            }

            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    Object grade = r.getObject("GRADE");

                    rows.add(new Object[]{
                            r.getInt("ID"),
                            r.getString("STUDENT_NO"),
                            r.getString("FIRST_NAME") + " "
                                    + r.getString("LAST_NAME"),
                            r.getString("COURSE_CODE") + " — "
                                    + r.getString("COURSE_NAME"),
                            r.getString("SCHOOL_YEAR"),
                            r.getString("SEMESTER"),
                            grade == null ? "" : r.getDouble("GRADE"),
                            r.getString("REMARKS") == null
                                    ? "" : r.getString("REMARKS"),
                            r.getInt("STUDENT_ID"),
                            r.getInt("COURSE_ID")
                    });
                }
            }
        }

        return rows;
    }

    /**
     * Creates or updates an enrollment.
     *
     * The general enrollment editor is restricted to Administrator
     * and Registrar. A non-null grade can only be written by an
     * Administrator through this legacy method; instructor grade entry
     * should use a separate section-assignment-checked grading DAO.
     */
    public void save(
            int id,
            int studentId,
            int courseId,
            String schoolYear,
            String semester,
            Double grade,
            String remarks) throws SQLException {

        UserSession user = AccessControl.requireAdminOrRegistrar();

        if (grade != null && !user.isAdmin()) {
            throw new SecurityException(
                    "Registrar accounts cannot edit grades through enrollment management.");
        }

        validateEnrollment(
                studentId, courseId, schoolYear, semester, grade, remarks);

        // Registrar edits enrollment placement only; existing grade and remarks are preserved.
        if (id != 0 && !user.isAdmin()) {
            String updateEnrollment = "UPDATE ENROLLMENTS SET STUDENT_ID=?, COURSE_ID=?, SCHOOL_YEAR=?, SEMESTER=? WHERE ID=?";
            try (Connection c = DatabaseConnection.getConnection(); PreparedStatement p = c.prepareStatement(updateEnrollment)) {
                p.setInt(1, studentId); p.setInt(2, courseId); p.setString(3, schoolYear.trim()); p.setString(4, semester.trim()); p.setInt(5, id);
                if (p.executeUpdate() == 0) throw new SQLException("Enrollment not found.");
            }
            return;
        }

        String sql;

        if (id == 0) {
            sql = """
                INSERT INTO ENROLLMENTS
                    (STUDENT_ID, COURSE_ID, SCHOOL_YEAR,
                     SEMESTER, GRADE, REMARKS)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        } else {
            sql = """
                UPDATE ENROLLMENTS
                SET STUDENT_ID = ?, COURSE_ID = ?, SCHOOL_YEAR = ?,
                    SEMESTER = ?, GRADE = ?, REMARKS = ?
                WHERE ID = ?
                """;
        }

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p = c.prepareStatement(sql)) {

            p.setInt(1, studentId);
            p.setInt(2, courseId);
            p.setString(3, schoolYear.trim());
            p.setString(4, semester.trim());

            if (grade == null) {
                p.setNull(5, Types.DOUBLE);
            } else {
                p.setDouble(5, grade);
            }

            if (remarks == null || remarks.isBlank()) {
                p.setNull(6, Types.VARCHAR);
            } else {
                p.setString(6, remarks.trim());
            }

            if (id != 0) {
                p.setInt(7, id);
            }

            int affected = p.executeUpdate();

            if (id != 0 && affected == 0) {
                throw new SQLException("Enrollment not found.");
            }
        }
    }

    public void delete(int id) throws SQLException {
        AccessControl.requireAdminOrRegistrar();

        if (id <= 0) {
            throw new IllegalArgumentException("Invalid enrollment ID.");
        }

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement p =
                     c.prepareStatement("DELETE FROM ENROLLMENTS WHERE ID = ?")) {

            p.setInt(1, id);

            if (p.executeUpdate() == 0) {
                throw new SQLException("Enrollment not found.");
            }
        }
    }

    public int count() throws SQLException {
        AccessControl.requireSignedIn();

        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery(
                     "SELECT COUNT(*) FROM ENROLLMENTS")) {

            r.next();
            return r.getInt(1);
        }
    }

    private static void validateEnrollment(
            int studentId,
            int courseId,
            String schoolYear,
            String semester,
            Double grade,
            String remarks) {

        if (studentId <= 0 || courseId <= 0) {
            throw new IllegalArgumentException(
                    "Select a valid student and course.");
        }

        if (schoolYear == null || schoolYear.isBlank()
                || schoolYear.trim().length() > 20) {
            throw new IllegalArgumentException(
                    "Enter a valid school year (maximum 20 characters).");
        }

        if (semester == null || semester.isBlank()
                || semester.trim().length() > 30) {
            throw new IllegalArgumentException(
                    "Select a valid semester.");
        }

        if (grade != null
                && (!Double.isFinite(grade) || grade < 0 || grade > 100)) {
            throw new IllegalArgumentException(
                    "Grade must be a finite number from 0 to 100.");
        }

        if (remarks != null && remarks.length() > 30) {
            throw new IllegalArgumentException(
                    "Remarks cannot exceed 30 characters.");
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
