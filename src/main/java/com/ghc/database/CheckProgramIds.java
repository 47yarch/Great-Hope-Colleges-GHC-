
package com.ghc.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckProgramIds {

    public static void main(String[] args) {
        String sql = "SELECT MAX(ID) AS MAX_PROGRAM_ID FROM PROGRAMS";

        try (Connection con = DatabaseConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                System.out.println(
                    "Highest program ID: " + rs.getInt("MAX_PROGRAM_ID")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
