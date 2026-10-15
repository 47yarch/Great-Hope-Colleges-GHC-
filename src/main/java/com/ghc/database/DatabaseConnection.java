
package com.ghc.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private static final String URL;

    static {
        try {
            // Reduce Derby identity sequence preallocation.
            // This must be set before Derby starts.
            System.setProperty(
                "derby.language.sequence.preallocator", "1"
            );

            Path databasePath = Path.of("ghc_database")
                    .toAbsolutePath();

            Files.createDirectories(databasePath);

            URL = "jdbc:derby:"
                    + databasePath.resolve("GHCDB")
                            .toString().replace('\\', '/')
                    + ";create=true";

            Class.forName("org.apache.derby.iapi.jdbc.AutoloadedDriver");

        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }
}
