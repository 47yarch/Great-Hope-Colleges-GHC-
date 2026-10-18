
package com.ghc.database;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;

public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    public static void initialize() throws Exception {
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement()) {

            // USERS
            create(s, """
                CREATE TABLE USERS (
                    ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                    USERNAME VARCHAR(50) NOT NULL UNIQUE,
                    PASSWORD_HASH VARCHAR(64) NOT NULL,
                    FULL_NAME VARCHAR(120) NOT NULL,
                    ROLE VARCHAR(20) NOT NULL
                )
                """);

            // STUDENTS
            create(s, """
                CREATE TABLE STUDENTS (
                    ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                    STUDENT_NO VARCHAR(30) NOT NULL UNIQUE,
                    FIRST_NAME VARCHAR(80) NOT NULL,
                    LAST_NAME VARCHAR(80) NOT NULL,
                    GENDER VARCHAR(20),
                    BIRTH_DATE VARCHAR(20),
                    EMAIL VARCHAR(120),
                    PHONE VARCHAR(40),
                    ADDRESS VARCHAR(250),
                    YEAR_LEVEL VARCHAR(30),
                    PROGRAM VARCHAR(100),
                    STATUS VARCHAR(20) DEFAULT 'Active'
                )
                """);

            // COURSES
            create(s, """
                CREATE TABLE COURSES (
                    ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                    COURSE_CODE VARCHAR(30) NOT NULL UNIQUE,
                    COURSE_NAME VARCHAR(120) NOT NULL,
                    UNITS INT NOT NULL,
                    DESCRIPTION VARCHAR(250)
                )
                """);

            // ENROLLMENTS
            create(s, """
                CREATE TABLE ENROLLMENTS (
                    ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                    STUDENT_ID INT NOT NULL,
                    COURSE_ID INT NOT NULL,
                    SCHOOL_YEAR VARCHAR(20) NOT NULL,
                    SEMESTER VARCHAR(30) NOT NULL,
                    GRADE DOUBLE,
                    REMARKS VARCHAR(30),
                    CONSTRAINT FK_ENR_STUDENT
                        FOREIGN KEY (STUDENT_ID) REFERENCES STUDENTS(ID),
                    CONSTRAINT FK_ENR_COURSE
                        FOREIGN KEY (COURSE_ID) REFERENCES COURSES(ID),
                    CONSTRAINT UQ_ENROLL
                        UNIQUE (STUDENT_ID, COURSE_ID, SCHOOL_YEAR, SEMESTER)
                )
                """);

            // Migrations for existing databases.
            ensureColumn(c, "USERS", "ACTIVE",
                    "ALTER TABLE USERS ADD COLUMN ACTIVE SMALLINT DEFAULT 1 NOT NULL");

            ensureColumn(c, "USERS", "STUDENT_ID",
                    "ALTER TABLE USERS ADD COLUMN STUDENT_ID INT");

            ensureColumn(c, "ENROLLMENTS", "SECTION_ID",
                    "ALTER TABLE ENROLLMENTS ADD COLUMN SECTION_ID INT");

            ensureColumn(c, "ENROLLMENTS", "GRADE_EQUIVALENT",
                    "ALTER TABLE ENROLLMENTS ADD COLUMN GRADE_EQUIVALENT VARCHAR(10)");

            ensureColumn(c, "ENROLLMENTS", "STATUS",
                    "ALTER TABLE ENROLLMENTS ADD COLUMN STATUS VARCHAR(20) DEFAULT 'APPROVED' NOT NULL");

            ensureColumn(c, "ENROLLMENTS", "REQUESTED_AT",
                    "ALTER TABLE ENROLLMENTS ADD COLUMN REQUESTED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL");

            ensureColumn(c, "ENROLLMENTS", "REVIEWED_BY",
                    "ALTER TABLE ENROLLMENTS ADD COLUMN REVIEWED_BY INT");

            ensureColumn(c, "ENROLLMENTS", "REVIEWED_AT",
                    "ALTER TABLE ENROLLMENTS ADD COLUMN REVIEWED_AT TIMESTAMP");

            // DEPARTMENTS
            create(s, """
                CREATE TABLE DEPARTMENTS (
                    ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                    DEPARTMENT_CODE VARCHAR(30) NOT NULL UNIQUE,
                    DEPARTMENT_NAME VARCHAR(120) NOT NULL,
                    DESCRIPTION VARCHAR(250),
                    ACTIVE SMALLINT DEFAULT 1 NOT NULL
                )
                """);

            // PROGRAMS
            create(s, """
                CREATE TABLE PROGRAMS (
                    ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                    PROGRAM_CODE VARCHAR(30) NOT NULL UNIQUE,
                    PROGRAM_NAME VARCHAR(120) NOT NULL,
                    DEPARTMENT_ID INT,
                    DESCRIPTION VARCHAR(250),
                    ACTIVE SMALLINT DEFAULT 1 NOT NULL,
                    CONSTRAINT FK_PROGRAM_DEPARTMENT
                        FOREIGN KEY (DEPARTMENT_ID) REFERENCES DEPARTMENTS(ID)
                )
                """);

            // CLASS SECTIONS
            create(s, """
                CREATE TABLE CLASS_SECTIONS (
                    ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                    SECTION_CODE VARCHAR(40) NOT NULL,
                    COURSE_ID INT NOT NULL,
                    INSTRUCTOR_USER_ID INT,
                    SCHOOL_YEAR VARCHAR(20) NOT NULL,
                    SEMESTER VARCHAR(30) NOT NULL,
                    CAPACITY INT DEFAULT 40 NOT NULL,
                    ACTIVE SMALLINT DEFAULT 1 NOT NULL,
                    CONSTRAINT FK_SECTION_COURSE
                        FOREIGN KEY (COURSE_ID) REFERENCES COURSES(ID),
                    CONSTRAINT FK_SECTION_INSTRUCTOR
                        FOREIGN KEY (INSTRUCTOR_USER_ID) REFERENCES USERS(ID),
                    CONSTRAINT UQ_SECTION_TERM
                        UNIQUE (SECTION_CODE, SCHOOL_YEAR, SEMESTER)
                )
                """);

            // SECTION SCHEDULES
            create(s, """
                CREATE TABLE SECTION_SCHEDULES (
                    ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                    SECTION_ID INT NOT NULL,
                    DAY_OF_WEEK VARCHAR(12) NOT NULL,
                    START_TIME VARCHAR(5) NOT NULL,
                    END_TIME VARCHAR(5) NOT NULL,
                    ROOM VARCHAR(80),
                    CONSTRAINT FK_SCHEDULE_SECTION
                        FOREIGN KEY (SECTION_ID) REFERENCES CLASS_SECTIONS(ID)
                )
                """);

            // GRADE COMPONENTS
            create(s, """
                CREATE TABLE GRADE_COMPONENTS (
                    ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                    ENROLLMENT_ID INT NOT NULL,
                    COMPONENT_TYPE VARCHAR(30) NOT NULL,
                    SCORE DOUBLE NOT NULL,
                    UPDATED_BY INT NOT NULL,
                    UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                    CONSTRAINT FK_GRADE_COMPONENT_ENROLLMENT
                        FOREIGN KEY (ENROLLMENT_ID) REFERENCES ENROLLMENTS(ID),
                    CONSTRAINT FK_GRADE_COMPONENT_USER
                        FOREIGN KEY (UPDATED_BY) REFERENCES USERS(ID),
                    CONSTRAINT UQ_GRADE_COMPONENT
                        UNIQUE (ENROLLMENT_ID, COMPONENT_TYPE),
                    CONSTRAINT CK_GRADE_COMPONENT_SCORE
                        CHECK (SCORE >= 0 AND SCORE <= 100)
                )
                """);

            // GRADE AUDIT
            create(s, """
                CREATE TABLE GRADE_AUDIT (
                    ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                    ENROLLMENT_ID INT NOT NULL,
                    COMPONENT_TYPE VARCHAR(30),
                    OLD_SCORE DOUBLE,
                    NEW_SCORE DOUBLE,
                    CHANGED_BY INT NOT NULL,
                    CHANGED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                    NOTE VARCHAR(250),
                    CONSTRAINT FK_GRADE_AUDIT_ENROLLMENT
                        FOREIGN KEY (ENROLLMENT_ID) REFERENCES ENROLLMENTS(ID),
                    CONSTRAINT FK_GRADE_AUDIT_USER
                        FOREIGN KEY (CHANGED_BY) REFERENCES USERS(ID)
                )
                """);

            // APP SETTINGS
            create(s, """
                CREATE TABLE APP_SETTINGS (
                    SETTING_KEY VARCHAR(100) PRIMARY KEY,
                    SETTING_VALUE VARCHAR(500)
                )
                """);

            // Seed initial records when absent.
            seedAdmin(c);
            seedCourse(c, "GEN101", "General Education", 3,
                    "General education elective");
            seedCourse(c, "MATH101", "College Mathematics", 3,
                    "Foundational mathematics");
        }
    }

    private static void create(Statement s, String sql)
            throws SQLException {
        try {
            s.executeUpdate(sql);
        } catch (SQLException e) {
            // Derby: table already exists.
            if (!"X0Y32".equals(e.getSQLState())) {
                throw e;
            }
        }
    }

    /**
     * Adds a column only when it is missing from the current schema.
     */
    
private static void ensureColumn(
        Connection c,
        String table,
        String column,
        String sql) throws SQLException {

    DatabaseMetaData md = c.getMetaData();

    String tableName = table.toUpperCase(java.util.Locale.ROOT);
    String columnName = column.toUpperCase(java.util.Locale.ROOT);

    // First check the connection's current schema.
    String schema = c.getSchema();

    if (columnExists(md, c, schema, tableName, columnName)) {
        return;
    }

    // Derby metadata may return a null or unexpected schema.
    if (columnExists(md, c, null, tableName, columnName)) {
        return;
    }

    try (Statement statement = c.createStatement()) {
        statement.executeUpdate(sql);
    } catch (SQLException ex) {
        // If the column was created concurrently, accept that result.
        if (columnExists(md, c, schema, tableName, columnName)
                || columnExists(md, c, null, tableName, columnName)) {
            return;
        }

        throw ex;
    }
}

private static boolean columnExists(
        DatabaseMetaData md,
        Connection c,
        String schema,
        String table,
        String column) throws SQLException {

    try (ResultSet rs = md.getColumns(
            c.getCatalog(), schema, table, column)) {

        while (rs.next()) {
            if (column.equalsIgnoreCase(rs.getString("COLUMN_NAME"))) {
                return true;
            }
        }
    }

    return false;
}


    private static void seedAdmin(Connection c) throws Exception {
        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO USERS " +
                "(USERNAME, PASSWORD_HASH, FULL_NAME, ROLE, ACTIVE) " +
                "VALUES (?, ?, ?, ?, 1)")) {

            p.setString(1, "admin");
            p.setString(2, hash("admin123"));
            p.setString(3, "GHC Administrator");
            p.setString(4, "ADMIN");
            p.executeUpdate();

        } catch (SQLException e) {
            // Ignore duplicate username only.
            if (!"23505".equals(e.getSQLState())) {
                throw e;
            }
        }
    }

    private static void seedCourse(
            Connection c,
            String code,
            String name,
            int units,
            String description) throws SQLException {

        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO COURSES " +
                "(COURSE_CODE, COURSE_NAME, UNITS, DESCRIPTION) " +
                "VALUES (?, ?, ?, ?)")) {

            p.setString(1, code);
            p.setString(2, name);
            p.setInt(3, units);
            p.setString(4, description);
            p.executeUpdate();

        } catch (SQLException e) {
            if (!"23505".equals(e.getSQLState())) {
                throw e;
            }
        }
    }

    public static String hash(String text) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8));

        StringBuilder out = new StringBuilder();

        for (byte b : bytes) {
            out.append(String.format("%02x", b));
        }

        return out.toString();
    }
}
