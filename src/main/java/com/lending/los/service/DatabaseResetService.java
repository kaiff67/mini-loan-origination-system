package com.lending.los.service;

import com.lending.los.dao.DBConnection;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;

/**
 * Utility service to reset database tables to baseline seed data.
 * Re-executes schema.sql and seed_data.sql for deterministic testing and development.
 */
public class DatabaseResetService {

    public static boolean resetToBenchmark() {
        System.out.println("[INFO] Resetting database to baseline dataset...");
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Execute schema.sql
            executeSqlScript(stmt, "/schema.sql");

            // 2. Execute seed_data.sql
            executeSqlScript(stmt, "/seed_data.sql");

            // 3. Reset simulated business date to benchmark baseline
            BusinessDateService.setBusinessDate(LocalDate.of(2026, 10, 23));

            System.out.println("[INFO] Database successfully reset to baseline seed state.");
            return true;
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to reset database: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static void executeSqlScript(Statement stmt, String resourcePath) throws Exception {
        InputStream is = DatabaseResetService.class.getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalStateException("Resource not found on classpath: " + resourcePath);
        }

        StringBuilder sqlBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                // Skip empty lines and SQL comment lines
                if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("/*")) {
                    continue;
                }
                sqlBuilder.append(line).append(" ");
                if (trimmed.endsWith(";")) {
                    String sql = sqlBuilder.toString().trim();
                    // Strip the trailing semicolon
                    if (sql.endsWith(";")) {
                        sql = sql.substring(0, sql.length() - 1).trim();
                    }
                    if (!sql.isEmpty()) {
                        try {
                            stmt.execute(sql);
                        } catch (Exception ex) {
                            // If table doesn't exist during DROP, ignore
                            if (!sql.toUpperCase().startsWith("DROP TABLE")) {
                                throw ex;
                            }
                        }
                    }
                    sqlBuilder.setLength(0);
                }
            }
        }
    }
}
