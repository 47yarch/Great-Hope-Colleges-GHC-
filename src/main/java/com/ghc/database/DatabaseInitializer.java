
package com.ghc.database;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;

public final class DatabaseInitializer {
    private DatabaseInitializer() {}

    public static void initialize() throws Exception {
        try (Connection c = DatabaseConnection.getConnection();
             Statement s = c.createStatement()) {

            // Existing tables: retain all existing rows and identity values.
            create(s, """
                CREATE TABLE USERS (
                    ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                    USERNAME VARCHAR(50) NOT NULL UNIQUE,
                    PASSWORD_HASH VARCHAR(64) NOT NULL,
                    FULL_NAME VARCHAR(120) NOT NULL,
                    ROLE VARCHAR(20) NOT NULL
                )
                """);

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

            create(s, """
                CREATE TABLE COURSES (
                    ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                    COURSE_CODE VARCHAR(30) NOT NULL UNIQUE,
                    COURSE_NAME VARCHAR(120) NOT NULL,
                    UNITS INT NOT NULL,
                    DESCRIPTION VARCHAR(250)
                )
                """);

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

            // Safe, repeatable migrations for databases created by older versions.
            ensureColumn(c, "USERS", "ACTIVE",
                    "ALTER TABLE USERS ADD COLUMN ACTIVE SMALLINT DEFAULT 1 NOT NULL");

            // Academic setup.
            create(s, """
                CREATE TABLE DEPARTMENTS (
                    ID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                    DEPARTMENT_CODE VARCHAR(30) NOT NULL UNIQUE,
                    DEPARTMENT_NAME VARCHAR(120) NOT NULL,
                    DESCRIPTION VARCHAR(250),
                    ACTIVE SMALLINT DEFAULT 1 NOT NULL
                )
                """);

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

            // A class section is a particular offering of a course in a term.
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

            // Existing enrollments can remain unassigned to a section until updated.
            ensureColumn(c, "ENROLLMENTS", "SECTION_ID",
                    "ALTER TABLE ENROLLMENTS ADD COLUMN SECTION_ID INT");
            ensureColumn(c, "ENROLLMENTS", "GRADE_EQUIVALENT",
                    "ALTER TABLE ENROLLMENTS ADD COLUMN GRADE_EQUIVALENT VARCHAR(10)");

            // Store individual grade components separately from the existing final grade.
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

            create(s, """
                CREATE TABLE APP_SETTINGS (
                    SETTING_KEY VARCHAR(100) PRIMARY KEY,
                    SETTING_VALUE VARCHAR(500)
                )
                """);

            seedAdmin(c);
            seedCourse(c, "GEN101", "General Education", 3,
                    "General education elective");
            seedCourse(c, "MATH101", "College Mathematics", 3,
                    "Foundational mathematics");
        }
    }

    private static void create(Statement s, String sql) throws SQLException {
        try {
            s.executeUpdate(sql);
        } catch (SQLException e) {
            // Derby SQLState X0Y32: table or view already exists.
            if (!"X0Y32".equals(e.getSQLState())) {
                throw e;
            }
        }
    }

    private static void ensureColumn(
            Connection c, String table, String column, String sql)
            throws SQLException {

        DatabaseMetaData md = c.getMetaData();

        try (ResultSet rs = md.getColumns(
                null, null, table.toUpperCase(), column.toUpperCase())) {
            if (rs.next()) {
                return;
            }
        }

        try (Statement s = c.createStatement()) {
            s.executeUpdate(sql);
        }
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
            // Duplicate username only; other errors must not be hidden.
            if (!"23505".equals(e.getSQLState())) {
                throw e;
            }
        }
    }

    private static void seedCourse(
            Connection c, String code, String name, int units, String desc)
            throws SQLException {

        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO COURSES " +
                "(COURSE_CODE, COURSE_NAME, UNITS, DESCRIPTION) " +
                "VALUES (?, ?, ?, ?)")) {

            p.setString(1, code);
            p.setString(2, name);
            p.setInt(3, units);
            p.setString(4, desc);
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
