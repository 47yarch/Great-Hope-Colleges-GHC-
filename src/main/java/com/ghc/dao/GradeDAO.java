
package com.ghc.dao;

import com.ghc.database.DatabaseConnection;
import com.ghc.grading.GradingCalculator;
import com.ghc.security.AccessControl;
import com.ghc.security.UserSession;

import java.sql.*;
import java.util.Map;

public final class GradeDAO {

    private static final String[] LECTURE_KEYS = {
        "LEC_WW", "LEC_UNIT_TEST", "LEC_TERM_TEST"
    };

    private static final String[] LAB_KEYS = {
        "LAB_WW", "LAB_UNIT_TEST", "LAB_TERM_TEST", "LAB_PERFORMANCE"
    };

    private static final String[] ALL_KEYS = {
        "LEC_WW", "LEC_UNIT_TEST", "LEC_TERM_TEST",
        "LAB_WW", "LAB_UNIT_TEST", "LAB_TERM_TEST", "LAB_PERFORMANCE"
    };

    public void saveCalculatedGrade(
            int enrollmentId,
            Map<String, Double> components,
            boolean hasLaboratory) throws SQLException {

        UserSession user = AccessControl.requireSignedIn();

        if (enrollmentId <= 0 || components == null) {
            throw new IllegalArgumentException(
                "A valid enrollment and component scores are required.");
        }

        double[] scores = new double[7];

        // Validate lecture scores, which are always required.
        for (int i = 0; i < LECTURE_KEYS.length; i++) {
            scores[i] = requireScore(components.get(LECTURE_KEYS[i]),
                    LECTURE_KEYS[i]);
        }

        // Laboratory scores are required only for courses with laboratory.
        for (int i = 0; i < LAB_KEYS.length; i++) {
            int index = i + 3;

            if (hasLaboratory) {
                scores[index] = requireScore(
                    components.get(LAB_KEYS[i]), LAB_KEYS[i]);
            } else {
                scores[index] = 0.0;
            }
        }

        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);

            try {
                int sectionId;

                try (PreparedStatement p = c.prepareStatement(
                        "SELECT SECTION_ID FROM ENROLLMENTS "
                        + "WHERE ID = ? FOR UPDATE")) {

                    p.setInt(1, enrollmentId);

                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) {
                            throw new SQLException("Enrollment not found.");
                        }

                        if (r.getObject(1) == null) {
                            throw new IllegalArgumentException(
                                "Assign this enrollment to a class section "
                                + "before grading.");
                        }

                        sectionId = r.getInt(1);
                    }
                }

                // Verify authorization inside the transaction.
                if (user.isInstructor()) {
                    try (PreparedStatement p = c.prepareStatement("""
                        SELECT COUNT(*)
                        FROM CLASS_SECTIONS
                        WHERE ID = ?
                          AND INSTRUCTOR_USER_ID = ?
                          AND ACTIVE = 1
                        """)) {

                        p.setInt(1, sectionId);
                        p.setInt(2, user.getUserId());

                        try (ResultSet r = p.executeQuery()) {
                            r.next();

                            if (r.getInt(1) == 0) {
                                throw new SecurityException(
                                    "You can only grade students in your "
                                    + "active assigned classes.");
                            }
                        }
                    }
                } else if (!user.isAdmin()) {
                    throw new SecurityException(
                        "Only an Administrator or assigned Instructor "
                        + "may edit grades.");
                }

                // Save lecture grades.
                for (int i = 0; i < LECTURE_KEYS.length; i++) {
                    saveComponent(
                        c, enrollmentId, LECTURE_KEYS[i],
                        scores[i], user.getUserId());
                }

                if (hasLaboratory) {
                    // Save laboratory grades only when enabled.
                    for (int i = 0; i < LAB_KEYS.length; i++) {
                        saveComponent(
                            c, enrollmentId, LAB_KEYS[i],
                            scores[i + 3], user.getUserId());
                    }
                } else {
                    // Remove old laboratory grades and audit their removal.
                    for (String key : LAB_KEYS) {
                        deleteComponent(
                            c, enrollmentId, key, user.getUserId());
                    }
                }

                double lecture = GradingCalculator.calculateLecture(
                    scores[0], scores[1], scores[2]);

                double laboratory = hasLaboratory
                    ? GradingCalculator.calculateLaboratory(
                        scores[3], scores[4], scores[5], scores[6])
                    : 0.0;

                double raw = GradingCalculator.finalRawGrade(
                    lecture, laboratory, hasLaboratory);

                try (PreparedStatement p = c.prepareStatement("""
                    UPDATE ENROLLMENTS
                    SET GRADE = ?, GRADE_EQUIVALENT = ?, REMARKS = ?
                    WHERE ID = ?
                    """)) {

                    p.setDouble(1, raw);
                    p.setString(2, GradingCalculator.equivalentGrade(raw));
                    p.setString(3, raw >= 50 ? "Passed" : "Failed");
                    p.setInt(4, enrollmentId);

                    if (p.executeUpdate() != 1) {
                        throw new SQLException(
                            "Unable to update the enrollment grade.");
                    }
                }

                c.commit();

            } catch (Exception ex) {
                c.rollback();

                if (ex instanceof SQLException sqlException) {
                    throw sqlException;
                }

                if (ex instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }

                throw new SQLException(ex);

            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private static double requireScore(Double score, String key) {
        if (score == null) {
            throw new IllegalArgumentException(
                "Missing required grade component: " + key);
        }

        if (!Double.isFinite(score) || score < 0 || score > 100) {
            throw new IllegalArgumentException(
                "Score for " + key + " must be between 0 and 100.");
        }

        return score;
    }

    private static void saveComponent(
            Connection c,
            int enrollmentId,
            String key,
            double score,
            int userId) throws SQLException {

        Double oldScore = findScore(c, enrollmentId, key);

        if (oldScore == null) {
            try (PreparedStatement p = c.prepareStatement("""
                INSERT INTO GRADE_COMPONENTS
                    (ENROLLMENT_ID, COMPONENT_TYPE, SCORE, UPDATED_BY)
                VALUES (?, ?, ?, ?)
                """)) {

                p.setInt(1, enrollmentId);
                p.setString(2, key);
                p.setDouble(3, score);
                p.setInt(4, userId);
                p.executeUpdate();
            }
        } else {
            try (PreparedStatement p = c.prepareStatement("""
                UPDATE GRADE_COMPONENTS
                SET SCORE = ?, UPDATED_BY = ?, UPDATED_AT = CURRENT_TIMESTAMP
                WHERE ENROLLMENT_ID = ? AND COMPONENT_TYPE = ?
                """)) {

                p.setDouble(1, score);
                p.setInt(2, userId);
                p.setInt(3, enrollmentId);
                p.setString(4, key);
                p.executeUpdate();
            }
        }

        audit(c, enrollmentId, key, oldScore, score, userId,
                "Saved through gradebook");
    }

    private static void deleteComponent(
            Connection c,
            int enrollmentId,
            String key,
            int userId) throws SQLException {

        Double oldScore = findScore(c, enrollmentId, key);

        // Nothing to remove if no laboratory score was previously saved.
        if (oldScore == null) {
            return;
        }

        try (PreparedStatement p = c.prepareStatement("""
            DELETE FROM GRADE_COMPONENTS
            WHERE ENROLLMENT_ID = ? AND COMPONENT_TYPE = ?
            """)) {

            p.setInt(1, enrollmentId);
            p.setString(2, key);
            p.executeUpdate();
        }

        audit(c, enrollmentId, key, oldScore, null, userId,
                "Laboratory component disabled; saved score removed");
    }

    private static Double findScore(
            Connection c, int enrollmentId, String key)
            throws SQLException {

        try (PreparedStatement p = c.prepareStatement("""
            SELECT SCORE FROM GRADE_COMPONENTS
            WHERE ENROLLMENT_ID = ? AND COMPONENT_TYPE = ?
            """)) {

            p.setInt(1, enrollmentId);
            p.setString(2, key);

            try (ResultSet r = p.executeQuery()) {
                return r.next() ? r.getDouble(1) : null;
            }
        }
    }

    private static void audit(
            Connection c,
            int enrollmentId,
            String key,
            Double oldScore,
            Double newScore,
            int userId,
            String note) throws SQLException {

        try (PreparedStatement p = c.prepareStatement("""
            INSERT INTO GRADE_AUDIT
                (ENROLLMENT_ID, COMPONENT_TYPE, OLD_SCORE, NEW_SCORE,
                 CHANGED_BY, NOTE)
            VALUES (?, ?, ?, ?, ?, ?)
            """)) {

            p.setInt(1, enrollmentId);
            p.setString(2, key);

            if (oldScore == null) {
                p.setNull(3, Types.DOUBLE);
            } else {
                p.setDouble(3, oldScore);
            }

            if (newScore == null) {
                p.setNull(4, Types.DOUBLE);
            } else {
                p.setDouble(4, newScore);
            }

            p.setInt(5, userId);
            p.setString(6, note);
            p.executeUpdate();
        }
    }
}
