
package com.ghc.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public final class ProgramIdentityTest {

    public static void main(String[] args) {
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                // Use a savepoint so the test can be rolled back.
                var savepoint = con.setSavepoint();

                String sql =
                    "INSERT INTO PROGRAMS " +
                    "(PROGRAM_CODE, PROGRAM_NAME, DEPARTMENT_ID, " +
                    "DESCRIPTION, ACTIVE) " +
                    "VALUES (?, ?, ?, ?, 1)";

                try (PreparedStatement ps = con.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, "DIAG_TEMP_001");
                    ps.setString(2, "Temporary Identity Diagnostic");
                    ps.setNull(3, java.sql.Types.INTEGER);
                    ps.setString(4, "Temporary test; will be rolled back.");

                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            System.out.println(
                                "Next generated program ID: "
                                + keys.getInt(1));
                        } else {
                            System.out.println(
                                "Derby did not return a generated key.");
                        }
                    }
                }

                con.rollback(savepoint);
                System.out.println(
                    "Test record rolled back; no test program was retained.");

            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ProgramIdentityTest() {
    }
}
