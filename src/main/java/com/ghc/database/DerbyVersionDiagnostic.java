
package com.ghc.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

public final class DerbyVersionDiagnostic {

    public static void main(String[] args) throws Exception {
        try (Connection con = DatabaseConnection.getConnection()) {
            DatabaseMetaData md = con.getMetaData();

            System.out.println("Derby product version: "
                    + md.getDatabaseProductVersion());
            System.out.println("Derby driver version: "
                    + md.getDriverVersion());
            System.out.println("JDBC URL: " + md.getURL());
            System.out.println("Database user: " + md.getUserName());
        }
    }

    private DerbyVersionDiagnostic() {
    }
}
