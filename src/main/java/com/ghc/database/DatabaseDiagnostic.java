
package com.ghc.database;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public final class DatabaseDiagnostic {

    public static void main(String[] args) {
        System.out.println("Working directory: "
                + Path.of("").toAbsolutePath());

        try (Connection con = DatabaseConnection.getConnection()) {
            DatabaseMetaData meta = con.getMetaData();

            System.out.println("Database: "
                    + meta.getDatabaseProductName());
            System.out.println("Database URL: "
                    + meta.getURL());

            System.out.println("\n--- PROGRAMS ---");

            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT ID, PROGRAM_NAME, DEPARTMENT_ID "
                         + "FROM PROGRAMS ORDER BY ID")) {

                while (rs.next()) {
                    System.out.printf(
                            "ID=%d, PROGRAM_NAME=%s, DEPARTMENT_ID=%d%n",
                            rs.getInt("ID"),
                            rs.getString("PROGRAM_NAME"),
                            rs.getInt("DEPARTMENT_ID"));
                }
            } catch (Exception e) {
                System.out.println(
                        "Could not read PROGRAMS: " + e.getMessage());
            }

            System.out.println("\nDiagnostic completed.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DatabaseDiagnostic() {
    }
}
