package com.lending.los.dao;

import com.lending.los.model.ApplicationHistory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Application History (Audit Trail).
 * Supports execution within explicit JDBC transactions.
 */
public class ApplicationHistoryDAO {

    public void logHistory(ApplicationHistory history, Connection conn) throws SQLException {
        String sql = "INSERT INTO application_history (application_id, action, old_status, new_status, performed_by, performed_at, remarks) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, history.getApplicationId());
            ps.setString(2, history.getAction());
            ps.setString(3, history.getOldStatus());
            ps.setString(4, history.getNewStatus());
            ps.setString(5, history.getPerformedBy());
            ps.setTimestamp(6, Timestamp.valueOf(history.getPerformedAt()));
            ps.setString(7, history.getRemarks());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    history.setHistoryId(rs.getLong(1));
                }
            }
        }
    }

    public List<ApplicationHistory> findByApplicationId(Long applicationId) throws SQLException {
        List<ApplicationHistory> list = new ArrayList<>();
        String sql = "SELECT * FROM application_history WHERE application_id = ? ORDER BY performed_at ASC, history_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ApplicationHistory h = new ApplicationHistory();
                    h.setHistoryId(rs.getLong("history_id"));
                    h.setApplicationId(rs.getLong("application_id"));
                    h.setAction(rs.getString("action"));
                    h.setOldStatus(rs.getString("old_status"));
                    h.setNewStatus(rs.getString("new_status"));
                    h.setPerformedBy(rs.getString("performed_by"));
                    Timestamp ts = rs.getTimestamp("performed_at");
                    if (ts != null) {
                        h.setPerformedAt(ts.toLocalDateTime());
                    }
                    h.setRemarks(rs.getString("remarks"));
                    list.add(h);
                }
            }
        }
        return list;
    }
}
