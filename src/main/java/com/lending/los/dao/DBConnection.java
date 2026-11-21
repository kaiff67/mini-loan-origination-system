package com.lending.los.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Manages database connectivity using pure JDBC and java.sql.DriverManager.
 * Reads database parameters from db.properties or falls back to robust defaults.
 */
public class DBConnection {

    private static String url;
    private static String user;
    private static String password;

    static {
        loadProperties();
    }

    private static void loadProperties() {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
                url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/loan_origination_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
                user = props.getProperty("db.user", "root");
                password = props.getProperty("db.password", "root");
            } else {
                url = "jdbc:mysql://localhost:3306/loan_origination_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
                user = "root";
                password = "root";
            }
            // Explicitly load MySQL JDBC driver class
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            System.err.println("[WARN] Could not load db.properties: " + e.getMessage() + ". Using default connection settings.");
            url = "jdbc:mysql://localhost:3306/loan_origination_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            user = "root";
            password = "root";
        }
    }

    /**
     * Obtains a new JDBC Connection from DriverManager.
     * Callers must manage and close this connection (e.g. using try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Checks if the database is reachable and prints exact cause if failed.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("\n[DB ERROR DETAILS] " + e.getMessage());
            return false;
        }
    }

    public static String getUrl() {
        return url;
    }

    public static String getUser() {
        return user;
    }
}
