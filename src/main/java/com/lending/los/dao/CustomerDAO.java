package com.lending.los.dao;

import com.lending.los.model.Customer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Customer operations using pure JDBC PreparedStatements.
 */
public class CustomerDAO {

    public Long createCustomer(Customer customer) throws SQLException {
        String sql = "INSERT INTO customer (name, date_of_birth, mobile, address, kyc_details, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customer.getName());
            ps.setDate(2, Date.valueOf(customer.getDateOfBirth()));
            ps.setString(3, customer.getMobile());
            ps.setString(4, customer.getAddress());
            ps.setString(5, customer.getKycDetails());
            ps.setTimestamp(6, Timestamp.valueOf(customer.getCreatedAt() != null ? customer.getCreatedAt() : java.time.LocalDateTime.now()));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    customer.setCustomerId(id);
                    return id;
                }
            }
        }
        return null;
    }

    public Optional<Customer> findById(Long customerId) throws SQLException {
        String sql = "SELECT * FROM customer WHERE customer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Customer> findByMobile(String mobile) throws SQLException {
        String sql = "SELECT * FROM customer WHERE mobile = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, mobile);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Searches customers by ID, name, or mobile.
     */
    public List<Customer> searchCustomers(String query) throws SQLException {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customer WHERE CAST(customer_id AS CHAR) = ? OR mobile = ? OR LOWER(name) LIKE ? ORDER BY customer_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, query);
            ps.setString(2, query);
            ps.setString(3, "%" + query.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public List<Customer> findAll() throws SQLException {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM customer ORDER BY customer_id ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    private Customer mapResultSet(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setCustomerId(rs.getLong("customer_id"));
        c.setName(rs.getString("name"));
        c.setDateOfBirth(rs.getDate("date_of_birth").toLocalDate());
        c.setMobile(rs.getString("mobile"));
        c.setAddress(rs.getString("address"));
        c.setKycDetails(rs.getString("kyc_details"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            c.setCreatedAt(ts.toLocalDateTime());
        }
        return c;
    }
}
