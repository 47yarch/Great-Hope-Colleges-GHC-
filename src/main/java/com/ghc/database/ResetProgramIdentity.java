
package com.ghc.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ResetProgramIdentity {

    public static void main(String[] args) {
        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement()) {

            // Verify the highest existing program ID.
            int maxId = 0;

            try (ResultSet rs = st.executeQuery(
                    "SELECT MAX(ID) FROM PROGRAMS")) {
                if (rs.next()) {
                    maxId = rs.getInt(1);
                }
            }

            System.out.println("Highest existing program ID: " + maxId);

            // Safety check: only proceed if the highest ID is still 101.
            if (maxId != 101) {
                System.out.println(
                    "RESET CANCELLED. The highest ID is not 101. "
                    + "No changes were made.");
                return;
            }

            // Restart identity generation after the highest existing ID.
            st.executeUpdate(
                "ALTER TABLE PROGRAMS ALTER COLUMN ID RESTART WITH 102");

            System.out.println(
                "SUCCESS: Program identity restarted at 102.");
            System.out.println(
                "Existing program records were not changed.");

        } catch (Exception e) {
            System.err.println("Unable to reset program identity.");
            e.printStackTrace();
        }
    }
}
