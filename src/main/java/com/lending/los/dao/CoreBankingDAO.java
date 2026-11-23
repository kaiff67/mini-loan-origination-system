package com.lending.los.dao;

import com.lending.los.model.CoreBankingRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for NG_CORE_BANKING integration.
 * Enforces: zero duplicate entries (enforced by UNIQUE ApplicationID) and auditability.
 */
public class CoreBankingDAO {

    public boolean pushRecord(CoreBankingRecord record, Connection conn) throws SQLException {
        String checkSql = "SELECT 1 FROM ng_core_banking WHERE ApplicationID = ?";
        try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setLong(1, record.getApplicationId());
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next()) {
                    return false; // Already pushed, avoid duplicate
                }
            }
        }

        String insertSql = "INSERT INTO ng_core_banking (CustomerID, ApplicationID, Customer_Name, Address, DOB, KYC_Details, Approved_Amount, Disbursed_Amount, Disbursed_Date) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, record.getCustomerId());
            ps.setLong(2, record.getApplicationId());
            ps.setString(3, record.getCustomerName());
            ps.setString(4, record.getAddress());
            ps.setDate(5, Date.valueOf(record.getDob()));
            ps.setString(6, record.getKycDetails());
            ps.setBigDecimal(7, record.getApprovedAmount());
            ps.setBigDecimal(8, record.getDisbursedAmount());
            ps.setTimestamp(9, Timestamp.valueOf(record.getDisbursedDate()));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    record.setCoreId(rs.getLong(1));
                }
            }
            return true;
        }
    }

    public List<CoreBankingRecord> findAll() throws SQLException {
        List<CoreBankingRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM ng_core_banking ORDER BY Disbursed_Date DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                CoreBankingRecord r = new CoreBankingRecord();
                r.setCoreId(rs.getLong("core_id"));
                r.setCustomerId(rs.getLong("CustomerID"));
                r.setApplicationId(rs.getLong("ApplicationID"));
                r.setCustomerName(rs.getString("Customer_Name"));
                r.setAddress(rs.getString("Address"));
                r.setDob(rs.getDate("DOB").toLocalDate());
                r.setKycDetails(rs.getString("KYC_Details"));
                r.setApprovedAmount(rs.getBigDecimal("Approved_Amount"));
                r.setDisbursedAmount(rs.getBigDecimal("Disbursed_Amount"));
                r.setDisbursedDate(rs.getTimestamp("Disbursed_Date").toLocalDateTime());
                list.add(r);
            }
        }
        return list;
    }
}
