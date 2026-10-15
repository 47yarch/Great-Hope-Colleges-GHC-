package com.ghc.database;
import java.sql.*;
import java.nio.file.*;
public final class DatabaseConnection {
  private static final String URL;
  static {
    try { Path p=Paths.get("ghc_database").toAbsolutePath(); Files.createDirectories(p); URL="jdbc:derby:"+p.resolve("GHCDB").toString().replace('\\','/')+";create=true"; }
    catch(Exception e){ throw new ExceptionInInitializerError(e); }
    try { Class.forName("org.apache.derby.iapi.jdbc.AutoloadedDriver"); } catch(ClassNotFoundException ignored) {}
  }
  private DatabaseConnection(){}
  public static Connection getConnection() throws SQLException { return DriverManager.getConnection(URL); }
}
