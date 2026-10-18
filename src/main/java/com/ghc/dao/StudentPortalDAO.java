
package com.ghc.dao;

import com.ghc.database.DatabaseConnection;
import com.ghc.security.AccessControl;
import com.ghc.security.SessionContext;
import com.ghc.security.UserSession;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentPortalDAO {

    private Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    public List<Object[]> availableSections() throws SQLException {
        List<Object[]> rows = new ArrayList<>();

        
String sql =
    "SELECT cs.ID, cs.SECTION_CODE, c.COURSE_CODE, c.COURSE_NAME, c.UNITS, " +
    "cs.SCHOOL_YEAR, cs.SEMESTER, cs.CAPACITY, " +
    "(SELECT COUNT(*) FROM ENROLLMENTS e " +
    " WHERE e.SECTION_ID = cs.ID AND e.STATUS = 'APPROVED') " +
    "FROM CLASS_SECTIONS cs " +
    "JOIN COURSES c ON c.ID = cs.COURSE_ID " +
    "WHERE cs.ACTIVE = 1 " +
    "ORDER BY cs.SCHOOL_YEAR DESC, cs.SEMESTER";


        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getInt(5), rs.getString(6),
                    rs.getString(7), rs.getInt(8), rs.getInt(9)
                });
            }
        }

        return rows;
    }

    public void requestEnrollment(int sectionId) throws SQLException {
        UserSession user = AccessControl.requireSignedIn();

        if (!user.isStudent() || user.getStudentId() <= 0) {
            throw new SecurityException("A linked student account is required.");
        }

        String sectionSql =
            "SELECT COURSE_ID, SCHOOL_YEAR, SEMESTER, CAPACITY " +
            "FROM CLASS_SECTIONS WHERE ID = ? AND ACTIVE = 1";

        String countSql =
            "SELECT COUNT(*) FROM ENROLLMENTS " +
            "WHERE SECTION_ID = ? AND STATUS = 'APPROVED'";

        String duplicateSql =
            "SELECT COUNT(*) FROM ENROLLMENTS " +
            "WHERE STUDENT_ID = ? AND COURSE_ID = ? " +
            "AND SCHOOL_YEAR = ? AND SEMESTER = ? " +
            "AND STATUS IN ('PENDING', 'APPROVED')";

        String insertSql =
            "INSERT INTO ENROLLMENTS " +
            "(STUDENT_ID, COURSE_ID, SCHOOL_YEAR, SEMESTER, " +
            "SECTION_ID, STATUS, REQUESTED_AT) " +
            "VALUES (?, ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP)";

        try (Connection con = getConnection()) {
            con.setAutoCommit(false);

            try {
                int courseId;
                String schoolYear;
                String semester;
                int capacity;

                try (PreparedStatement ps = con.prepareStatement(sectionSql)) {
                    ps.setInt(1, sectionId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Section not found or inactive.");
                        }

                        courseId = rs.getInt(1);
                        schoolYear = rs.getString(2);
                        semester = rs.getString(3);
                        capacity = rs.getInt(4);
                    }
                }

                int enrolled;
                try (PreparedStatement ps = con.prepareStatement(countSql)) {
                    ps.setInt(1, sectionId);

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        enrolled = rs.getInt(1);
                    }
                }

                if (capacity > 0 && enrolled >= capacity) {
                    throw new SQLException("This section is already full.");
                }

                try (PreparedStatement ps = con.prepareStatement(duplicateSql)) {
                    ps.setInt(1, user.getStudentId());
                    ps.setInt(2, courseId);
                    ps.setString(3, schoolYear);
                    ps.setString(4, semester);

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        if (rs.getInt(1) > 0) {
                            throw new SQLException(
                                "You already have a pending request or approved enrollment for this course and term."
                            );
                        }
                    }
                }

                try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                    ps.setInt(1, user.getStudentId());
                    ps.setInt(2, courseId);
                    ps.setString(3, schoolYear);
                    ps.setString(4, semester);
                    ps.setInt(5, sectionId);
                    ps.executeUpdate();
                }

                con.commit();
            } catch (SQLException | RuntimeException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public List<Object[]> myEnrollments() throws SQLException {
        UserSession user = AccessControl.requireSignedIn();
        requireStudent(user);

        List<Object[]> rows = new ArrayList<>();

        
String sql =
    "SELECT e.ID, c.COURSE_CODE, c.COURSE_NAME, cs.SECTION_CODE, " +
    "e.SCHOOL_YEAR, e.SEMESTER, e.STATUS, e.GRADE, e.REMARKS " +
    "FROM ENROLLMENTS e " +
    "JOIN COURSES c ON c.ID = e.COURSE_ID " +
    "LEFT JOIN CLASS_SECTIONS cs ON cs.ID = e.SECTION_ID " +
    "WHERE e.STUDENT_ID = ? " +
    "ORDER BY e.REQUESTED_AT DESC";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, user.getStudentId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                        rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6),
                        rs.getString(7), rs.getObject(8), rs.getString(9)
                    });
                }
            }
        }

        return rows;
    }

    public List<Object[]> gradeReport() throws SQLException {
        UserSession user = AccessControl.requireSignedIn();
        requireStudent(user);

        List<Object[]> rows = new ArrayList<>();

       
String sql =
    "SELECT c.COURSE_CODE, c.COURSE_NAME, c.UNITS, e.GRADE, " +
    "e.GRADE_EQUIVALENT, e.SCHOOL_YEAR, e.SEMESTER " +
    "FROM ENROLLMENTS e " +
    "JOIN COURSES c ON c.ID = e.COURSE_ID " +
    "WHERE e.STUDENT_ID = ? AND e.STATUS = 'APPROVED' " +
    "ORDER BY e.SCHOOL_YEAR, e.SEMESTER, c.COURSE_CODE";


        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, user.getStudentId());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Object[]{
                        rs.getString(1), rs.getString(2), rs.getInt(3),
                        rs.getObject(4), rs.getObject(5),
                        rs.getString(6), rs.getString(7)
                    });
                }
            }
        }

        return rows;
    }

    public Object[] studentInfo() throws SQLException {
        UserSession user = AccessControl.requireSignedIn();
        requireStudent(user);

        String sql =
    "SELECT STUDENT_NO, FIRST_NAME, LAST_NAME, PROGRAM, YEAR_LEVEL " +
    "FROM STUDENTS WHERE ID = ?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, user.getStudentId());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Linked student record was not found.");
                }

                
return new Object[]{
    rs.getString(1),
    rs.getString(2) + " " + rs.getString(3),
    rs.getString(4),
    rs.getString(5)
};

            }
        }
    }

    

public List<Object[]> pendingRequests(String filter) throws SQLException {
    
String sql =
    "SELECT e.ID, s.STUDENT_NO, " +
    "TRIM(s.FIRST_NAME || ' ' || s.LAST_NAME) AS FULL_NAME, " +
    "c.COURSE_CODE, cs.SECTION_CODE, " +
    "e.SCHOOL_YEAR, e.SEMESTER, e.STATUS " +
    "FROM ENROLLMENTS e " +
    "JOIN STUDENTS s ON s.ID = e.STUDENT_ID " +
    "JOIN COURSES c ON c.ID = e.COURSE_ID " +
    "LEFT JOIN CLASS_SECTIONS cs ON cs.ID = e.SECTION_ID " +
    "WHERE e.STATUS IN ('PENDING', 'REJECTED') " +
    "AND (LOWER(s.STUDENT_NO) LIKE ? " +
    "OR LOWER(s.FIRST_NAME || ' ' || s.LAST_NAME) LIKE ? " +
    "OR LOWER(c.COURSE_CODE) LIKE ?) " +
    "ORDER BY e.REQUESTED_AT DESC";


    List<Object[]> requests = new ArrayList<>();
    String search = "%" + (filter == null ? "" : filter.trim().toLowerCase()) + "%";

    try (Connection c = DatabaseConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {

        ps.setString(1, search);
        ps.setString(2, search);
        ps.setString(3, search);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                requests.add(new Object[]{
                    rs.getInt("ID"),
                    rs.getString("STUDENT_NO"),
                    rs.getString("FULL_NAME"),
                    rs.getString("COURSE_CODE"),
                    rs.getString("SECTION_CODE"),
                    rs.getString("SCHOOL_YEAR"),
                    rs.getString("SEMESTER"),
                    rs.getString("STATUS")
                });
            }
        }
    }

    return requests;
}



    public void review(int enrollmentId, boolean approve) throws SQLException {
        UserSession reviewer = AccessControl.requireAdminOrRegistrar();

        String sql =
            "UPDATE ENROLLMENTS SET STATUS = ?, REVIEWED_BY = ?, " +
            "REVIEWED_AT = CURRENT_TIMESTAMP " +
            "WHERE ID = ? AND STATUS IN ('PENDING', 'REJECTED')";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, approve ? "APPROVED" : "REJECTED");
            ps.setInt(2, reviewer.getUserId());
            ps.setInt(3, enrollmentId);

            if (ps.executeUpdate() == 0) {
                throw new SQLException(
                    "Request not found or it has already been processed."
                );
            }
        }
    }

    private static void requireStudent(UserSession user) {
        if (!user.isStudent() || user.getStudentId() <= 0) {
            throw new SecurityException("Student access is required.");
        }
    }
}
