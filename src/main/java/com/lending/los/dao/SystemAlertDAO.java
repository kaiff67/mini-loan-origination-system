package com.lending.los.dao;

import com.lending.los.model.SystemAlert;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for System Alerts.
 * Enforces idempotency via UNIQUE constraint on (application_id, alert_type).
 */
public class SystemAlertDAO {

    /**
     * Creates an alert if it does not already exist.
     * Returns true if a new alert was created, false if it already existed.
     */
    public boolean createAlertIfNotExists(SystemAlert alert) throws SQLException {
        String checkSql = "SELECT 1 FROM system_alerts WHERE application_id = ? AND alert_type = ?";
        String insertSql = "INSERT INTO system_alerts (application_id, alert_type, created_at) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setLong(1, alert.getApplicationId());
                checkPs.setString(2, alert.getAlertType());
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        // Already exists, do not duplicate
                        return false;
                    }
                }
            }

            try (PreparedStatement insertPs = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertPs.setLong(1, alert.getApplicationId());
                insertPs.setString(2, alert.getAlertType());
                insertPs.setTimestamp(3, Timestamp.valueOf(alert.getCreatedAt()));
                insertPs.executeUpdate();
                try (ResultSet rs = insertPs.getGeneratedKeys()) {
                    if (rs.next()) {
                        alert.setAlertId(rs.getLong(1));
                    }
                }
                return true;
            } catch (SQLIntegrityConstraintViolationException e) {
                // Caught race-condition / DB constraint
                return false;
            }
        }
    }

    public List<SystemAlert> findByApplicationId(Long applicationId) throws SQLException {
        List<SystemAlert> list = new ArrayList<>();
        String sql = "SELECT * FROM system_alerts WHERE application_id = ? ORDER BY created_at ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SystemAlert a = new SystemAlert();
                    a.setAlertId(rs.getLong("alert_id"));
                    a.setApplicationId(rs.getLong("application_id"));
                    a.setAlertType(rs.getString("alert_type"));
                    a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    list.add(a);
                }
            }
        }
        return list;
    }

    public List<SystemAlert> findAll() throws SQLException {
        List<SystemAlert> list = new ArrayList<>();
        String sql = "SELECT * FROM system_alerts ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                SystemAlert a = new SystemAlert();
                a.setAlertId(rs.getLong("alert_id"));
                a.setApplicationId(rs.getLong("application_id"));
                a.setAlertType(rs.getString("alert_type"));
                a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                list.add(a);
            }
        }
        return list;
    }
}
