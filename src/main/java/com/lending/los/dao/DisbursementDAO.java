package com.lending.los.dao;

import com.lending.los.model.Disbursement;

import java.sql.*;
import java.util.Optional;

/**
 * Data Access Object for Disbursement operations.
 * Enforces: one disbursement per application and globally unique reference.
 */
public class DisbursementDAO {

    public void createDisbursement(Disbursement disbursement, Connection conn) throws SQLException {
        String sql = "INSERT INTO disbursement (application_id, reference_number, amount, disbursed_at) " +
                     "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, disbursement.getApplicationId());
            ps.setString(2, disbursement.getReferenceNumber());
            ps.setBigDecimal(3, disbursement.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(disbursement.getDisbursedAt()));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    disbursement.setDisbursementId(rs.getLong(1));
                }
            }
        }
    }

    public Optional<Disbursement> findByApplicationId(Long applicationId) throws SQLException {
        String sql = "SELECT * FROM disbursement WHERE application_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Disbursement d = new Disbursement();
                    d.setDisbursementId(rs.getLong("disbursement_id"));
                    d.setApplicationId(rs.getLong("application_id"));
                    d.setReferenceNumber(rs.getString("reference_number"));
                    d.setAmount(rs.getBigDecimal("amount"));
                    d.setDisbursedAt(rs.getTimestamp("disbursed_at").toLocalDateTime());
                    return Optional.of(d);
                }
            }
        }
        return Optional.empty();
    }

    public boolean isReferenceUnique(String referenceNumber) throws SQLException {
        String sql = "SELECT 1 FROM disbursement WHERE reference_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, referenceNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return !rs.next();
            }
        }
    }
}
