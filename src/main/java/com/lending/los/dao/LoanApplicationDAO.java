package com.lending.los.dao;

import com.lending.los.model.ApplicationStage;
import com.lending.los.model.LoanApplication;
import com.lending.los.model.LoanProduct;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Loan Application lifecycle and state persistence.
 */
public class LoanApplicationDAO {

    public Long createDraft(LoanApplication app) throws SQLException {
        String sql = "INSERT INTO loan_application (application_number, customer_id, product, branch_code, branch_name, " +
                     "requested_amount, tenure_months, monthly_income, existing_monthly_obligations, annual_interest_rate, " +
                     "proposed_emi, foir_percentage, eligible, eligibility_reasons, status, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, app.getApplicationNumber());
            ps.setLong(2, app.getCustomerId());
            ps.setString(3, app.getProduct().name());
            ps.setString(4, app.getBranchCode());
            ps.setString(5, app.getBranchName());
            ps.setBigDecimal(6, app.getRequestedAmount());
            ps.setInt(7, app.getTenureMonths());
            ps.setBigDecimal(8, app.getMonthlyIncome());
            ps.setBigDecimal(9, app.getExistingMonthlyObligations());
            ps.setBigDecimal(10, app.getAnnualInterestRate());
            ps.setBigDecimal(11, app.getProposedEmi());
            ps.setBigDecimal(12, app.getFoirPercentage());
            ps.setBoolean(13, app.isEligible());
            ps.setString(14, app.getEligibilityReasons());
            ps.setString(15, app.getStatus().name());
            ps.setTimestamp(16, Timestamp.valueOf(app.getCreatedAt()));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    app.setApplicationId(id);
                    return id;
                }
            }
        }
        return null;
    }

    public void update(LoanApplication app, Connection conn) throws SQLException {
        String sql = "UPDATE loan_application SET " +
                     "requested_amount = ?, tenure_months = ?, monthly_income = ?, existing_monthly_obligations = ?, " +
                     "proposed_emi = ?, foir_percentage = ?, eligible = ?, eligibility_reasons = ?, " +
                     "status = ?, submitted_at = ?, decided_at = ?, reviewer_code = ?, decision_reason = ? " +
                     "WHERE application_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, app.getRequestedAmount());
            ps.setInt(2, app.getTenureMonths());
            ps.setBigDecimal(3, app.getMonthlyIncome());
            ps.setBigDecimal(4, app.getExistingMonthlyObligations());
            ps.setBigDecimal(5, app.getProposedEmi());
            ps.setBigDecimal(6, app.getFoirPercentage());
            ps.setBoolean(7, app.isEligible());
            ps.setString(8, app.getEligibilityReasons());
            ps.setString(9, app.getStatus().name());
            ps.setTimestamp(10, app.getSubmittedAt() != null ? Timestamp.valueOf(app.getSubmittedAt()) : null);
            ps.setTimestamp(11, app.getDecidedAt() != null ? Timestamp.valueOf(app.getDecidedAt()) : null);
            ps.setString(12, app.getReviewerCode());
            ps.setString(13, app.getDecisionReason());
            ps.setLong(14, app.getApplicationId());

            ps.executeUpdate();
        }
    }

    public Optional<LoanApplication> findById(Long applicationId) throws SQLException {
        String sql = "SELECT a.*, c.name AS customer_name, c.mobile AS customer_mobile " +
                     "FROM loan_application a " +
                     "JOIN customer c ON a.customer_id = c.customer_id " +
                     "WHERE a.application_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<LoanApplication> findByApplicationNumber(String appNumber) throws SQLException {
        String sql = "SELECT a.*, c.name AS customer_name, c.mobile AS customer_mobile " +
                     "FROM loan_application a " +
                     "JOIN customer c ON a.customer_id = c.customer_id " +
                     "WHERE a.application_number = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, appNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if customer already has an ACTIVE application for the given product.
     * Active stages = DRAFT, UNDER_REVIEW, CREDIT_APPROVAL.
     */
    public boolean hasActiveApplication(Long customerId, LoanProduct product) throws SQLException {
        String sql = "SELECT 1 FROM loan_application " +
                     "WHERE customer_id = ? AND product = ? AND status IN ('DRAFT', 'UNDER_REVIEW', 'CREDIT_APPROVAL')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            ps.setString(2, product.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<LoanApplication> findByStage(ApplicationStage stage) throws SQLException {
        List<LoanApplication> list = new ArrayList<>();
        String sql = "SELECT a.*, c.name AS customer_name, c.mobile AS customer_mobile " +
                     "FROM loan_application a " +
                     "JOIN customer c ON a.customer_id = c.customer_id " +
                     "WHERE a.status = ? ORDER BY a.created_at ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stage.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        }
        return list;
    }

    public List<LoanApplication> findAll() throws SQLException {
        List<LoanApplication> list = new ArrayList<>();
        String sql = "SELECT a.*, c.name AS customer_name, c.mobile AS customer_mobile " +
                     "FROM loan_application a " +
                     "JOIN customer c ON a.customer_id = c.customer_id " +
                     "ORDER BY a.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    private LoanApplication mapResultSet(ResultSet rs) throws SQLException {
        LoanApplication app = new LoanApplication();
        app.setApplicationId(rs.getLong("application_id"));
        app.setApplicationNumber(rs.getString("application_number"));
        app.setCustomerId(rs.getLong("customer_id"));
        app.setProduct(LoanProduct.valueOf(rs.getString("product")));
        app.setBranchCode(rs.getString("branch_code"));
        app.setBranchName(rs.getString("branch_name"));
        app.setRequestedAmount(rs.getBigDecimal("requested_amount"));
        app.setTenureMonths(rs.getInt("tenure_months"));
        app.setMonthlyIncome(rs.getBigDecimal("monthly_income"));
        app.setExistingMonthlyObligations(rs.getBigDecimal("existing_monthly_obligations"));
        app.setAnnualInterestRate(rs.getBigDecimal("annual_interest_rate"));
        app.setProposedEmi(rs.getBigDecimal("proposed_emi"));
        app.setFoirPercentage(rs.getBigDecimal("foir_percentage"));
        app.setEligible(rs.getBoolean("eligible"));
        app.setEligibilityReasons(rs.getString("eligibility_reasons"));
        app.setStatus(ApplicationStage.valueOf(rs.getString("status")));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) app.setCreatedAt(created.toLocalDateTime());
        Timestamp submitted = rs.getTimestamp("submitted_at");
        if (submitted != null) app.setSubmittedAt(submitted.toLocalDateTime());
        Timestamp decided = rs.getTimestamp("decided_at");
        if (decided != null) app.setDecidedAt(decided.toLocalDateTime());

        app.setReviewerCode(rs.getString("reviewer_code"));
        app.setDecisionReason(rs.getString("decision_reason"));
        app.setCustomerName(rs.getString("customer_name"));
        app.setCustomerMobile(rs.getString("customer_mobile"));

        return app;
    }
}
