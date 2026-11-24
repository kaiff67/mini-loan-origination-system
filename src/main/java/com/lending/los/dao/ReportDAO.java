package com.lending.los.dao;

import com.lending.los.service.FinancialCalculator;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Executes parameterized analytical SQL queries for portfolio reporting and operational risk analysis.
 */
public class ReportDAO {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    /**
     * Report 1: Application Register
     * Filters by product and status.
     */
    public void printApplicationRegister(String productFilter, String statusFilter) throws SQLException {
        String sql = "SELECT a.application_number, c.name AS customer_name, a.product, a.requested_amount, a.proposed_emi, a.status, a.submitted_at " +
                     "FROM loan_application a " +
                     "JOIN customer c ON a.customer_id = c.customer_id " +
                     "WHERE (? IS NULL OR ? = 'ALL' OR a.product = ?) " +
                     "  AND (? IS NULL OR ? = 'ALL' OR a.status = ?) " +
                     "ORDER BY a.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, productFilter);
            ps.setString(2, productFilter);
            ps.setString(3, productFilter);
            ps.setString(4, statusFilter);
            ps.setString(5, statusFilter);
            ps.setString(6, statusFilter);

            System.out.println("\n==========================================================================================================");
            System.out.println(" REPORT 1: APPLICATION REGISTER (Product: " + (productFilter != null ? productFilter : "ALL") +
                               ", Status: " + (statusFilter != null ? statusFilter : "ALL") + ")");
            System.out.println("==========================================================================================================");
            System.out.printf("%-20s | %-18s | %-10s | %14s | %12s | %-15s | %-18s%n",
                    "Application No", "Customer Name", "Product", "Amount (INR)", "EMI (INR)", "Status", "Submitted At");
            System.out.println("----------------------------------------------------------------------------------------------------------");

            int count = 0;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    count++;
                    Timestamp ts = rs.getTimestamp("submitted_at");
                    String dateStr = (ts != null) ? ts.toLocalDateTime().format(DATE_FMT) : "Not Submitted";
                    System.out.printf("%-20s | %-18s | %-10s | %14s | %12s | %-15s | %-18s%n",
                            rs.getString("application_number"),
                            rs.getString("customer_name"),
                            rs.getString("product"),
                            FinancialCalculator.formatIndianCurrency(rs.getBigDecimal("requested_amount")),
                            FinancialCalculator.formatIndianCurrency(rs.getBigDecimal("proposed_emi")),
                            rs.getString("status"),
                            dateStr);
                }
            }
            System.out.println("----------------------------------------------------------------------------------------------------------");
            System.out.println("Total Applications Listed: " + count);
        }
    }

    /**
     * Report 2: Product Pipeline
     * 2A: Grouped by Product and Status.
     * 2B: Grouped by Branch and Product.
     */
    public void printProductPipeline() throws SQLException {
        String sqlProduct = "SELECT product, status, COUNT(application_id) AS app_count, COALESCE(SUM(requested_amount), 0.00) AS total_amount " +
                            "FROM loan_application GROUP BY product, status ORDER BY product, status";

        String sqlBranch = "SELECT branch_code, branch_name, product, COUNT(application_id) AS app_count, COALESCE(SUM(requested_amount), 0.00) AS total_amount " +
                           "FROM loan_application GROUP BY branch_code, branch_name, product ORDER BY branch_code, product";

        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("\n===================================================================================");
            System.out.println(" REPORT 2A: PIPELINE BY PRODUCT & STATUS");
            System.out.println("===================================================================================");
            System.out.printf("%-12s | %-18s | %10s | %20s%n", "Product", "Status", "Count", "Total Amount (INR)");
            System.out.println("-----------------------------------------------------------------------------------");
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlProduct)) {
                while (rs.next()) {
                    System.out.printf("%-12s | %-18s | %10d | %20s%n",
                            rs.getString("product"),
                            rs.getString("status"),
                            rs.getInt("app_count"),
                            FinancialCalculator.formatIndianCurrency(rs.getBigDecimal("total_amount")));
                }
            }

            System.out.println("\n===================================================================================");
            System.out.println(" REPORT 2B: PIPELINE BY BRANCH & PRODUCT");
            System.out.println("===================================================================================");
            System.out.printf("%-12s | %-24s | %-10s | %10s | %20s%n", "Branch Code", "Branch Name", "Product", "Count", "Total Amount (INR)");
            System.out.println("-----------------------------------------------------------------------------------");
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sqlBranch)) {
                while (rs.next()) {
                    System.out.printf("%-12s | %-24s | %-10s | %10d | %20s%n",
                            rs.getString("branch_code"),
                            rs.getString("branch_name"),
                            rs.getString("product"),
                            rs.getInt("app_count"),
                            FinancialCalculator.formatIndianCurrency(rs.getBigDecimal("total_amount")));
                }
            }
        }
    }

    /**
     * Report 3: Daily / Monthly / Quarterly Disbursement Summary
     * Date-wise and Branch-wise reports for supplied date range.
     */
    public void printDisbursementSummary(LocalDateTime fromDate, LocalDateTime toDate) throws SQLException {
        String sqlDate = "SELECT DATE(disbursed_at) AS disb_date, COUNT(disbursement_id) AS disb_count, COALESCE(SUM(amount), 0.00) AS total_amount " +
                         "FROM disbursement WHERE disbursed_at BETWEEN ? AND ? GROUP BY DATE(disbursed_at) ORDER BY disb_date ASC";

        String sqlBranch = "SELECT a.branch_code, a.branch_name, COUNT(d.disbursement_id) AS disb_count, COALESCE(SUM(d.amount), 0.00) AS total_amount " +
                          "FROM disbursement d JOIN loan_application a ON d.application_id = a.application_id " +
                          "WHERE d.disbursed_at BETWEEN ? AND ? GROUP BY a.branch_code, a.branch_name ORDER BY a.branch_code ASC";

        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("\n===================================================================================");
            System.out.println(" REPORT 3A: DISBURSEMENTS BY DATE (" + fromDate.toLocalDate() + " to " + toDate.toLocalDate() + ")");
            System.out.println("===================================================================================");
            System.out.printf("%-15s | %10s | %20s%n", "Disbursed Date", "Count", "Total Disbursed (INR)");
            System.out.println("-----------------------------------------------------------------------------------");
            try (PreparedStatement ps = conn.prepareStatement(sqlDate)) {
                ps.setTimestamp(1, Timestamp.valueOf(fromDate));
                ps.setTimestamp(2, Timestamp.valueOf(toDate));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        System.out.printf("%-15s | %10d | %20s%n",
                                rs.getDate("disb_date").toString(),
                                rs.getInt("disb_count"),
                                FinancialCalculator.formatIndianCurrency(rs.getBigDecimal("total_amount")));
                    }
                }
            }

            System.out.println("\n===================================================================================");
            System.out.println(" REPORT 3B: DISBURSEMENTS BY BRANCH (" + fromDate.toLocalDate() + " to " + toDate.toLocalDate() + ")");
            System.out.println("===================================================================================");
            System.out.printf("%-12s | %-24s | %10s | %20s%n", "Branch Code", "Branch Name", "Count", "Total Disbursed (INR)");
            System.out.println("-----------------------------------------------------------------------------------");
            try (PreparedStatement ps = conn.prepareStatement(sqlBranch)) {
                ps.setTimestamp(1, Timestamp.valueOf(fromDate));
                ps.setTimestamp(2, Timestamp.valueOf(toDate));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        System.out.printf("%-12s | %-24s | %10d | %20s%n",
                                rs.getString("branch_code"),
                                rs.getString("branch_name"),
                                rs.getInt("disb_count"),
                                FinancialCalculator.formatIndianCurrency(rs.getBigDecimal("total_amount")));
                    }
                }
            }
        }
    }

    /**
     * Report 4: Overdue Reviews
     * Current UNDER_REVIEW applications older than 2 calendar days from business date.
     */
    public void printOverdueReviews(LocalDate currentBusinessDate) throws SQLException {
        String sql = "SELECT a.application_number, c.name AS customer_name, c.mobile, a.requested_amount, a.submitted_at, " +
                     "DATEDIFF(?, DATE(a.submitted_at)) AS days_waiting " +
                     "FROM loan_application a " +
                     "JOIN customer c ON a.customer_id = c.customer_id " +
                     "WHERE a.status = 'UNDER_REVIEW' " +
                     "  AND DATEDIFF(?, DATE(a.submitted_at)) > 2 " +
                     "ORDER BY days_waiting DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(currentBusinessDate));
            ps.setDate(2, Date.valueOf(currentBusinessDate));

            System.out.println("\n===============================================================================================");
            System.out.println(" REPORT 4: OVERDUE REVIEWS (> 2 Days Waiting as of " + currentBusinessDate + ")");
            System.out.println("===============================================================================================");
            System.out.printf("%-20s | %-18s | %-12s | %14s | %-18s | %12s%n",
                    "Application No", "Customer Name", "Mobile", "Amount (INR)", "Submitted At", "Days Waiting");
            System.out.println("-----------------------------------------------------------------------------------------------");

            int count = 0;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    count++;
                    Timestamp ts = rs.getTimestamp("submitted_at");
                    String dateStr = (ts != null) ? ts.toLocalDateTime().format(DATE_FMT) : "";
                    System.out.printf("%-20s | %-18s | %-12s | %14s | %-18s | %12d%n",
                            rs.getString("application_number"),
                            rs.getString("customer_name"),
                            rs.getString("mobile"),
                            FinancialCalculator.formatIndianCurrency(rs.getBigDecimal("requested_amount")),
                            dateStr,
                            rs.getInt("days_waiting"));
                }
            }
            System.out.println("-----------------------------------------------------------------------------------------------");
            System.out.println("Total Overdue Applications: " + count);
        }
    }

    /**
     * Report 5: Customers without Disbursement
     * Customers with >= 1 application and NO disbursement across any application.
     */
    public void printCustomersWithoutDisbursement() throws SQLException {
        String sql = "SELECT DISTINCT c.customer_id, c.name, c.mobile, c.date_of_birth " +
                     "FROM customer c " +
                     "JOIN loan_application a ON c.customer_id = a.customer_id " +
                     "WHERE c.customer_id NOT IN (" +
                     "    SELECT DISTINCT app.customer_id " +
                     "    FROM loan_application app " +
                     "    JOIN disbursement d ON app.application_id = d.application_id" +
                     ") ORDER BY c.customer_id ASC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n===================================================================================");
            System.out.println(" REPORT 5: CUSTOMERS WITH APPLICATIONS BUT NO DISBURSEMENT");
            System.out.println("===================================================================================");
            System.out.printf("%-12s | %-22s | %-12s | %-12s%n", "Customer ID", "Customer Name", "Mobile", "DOB");
            System.out.println("-----------------------------------------------------------------------------------");

            int count = 0;
            while (rs.next()) {
                count++;
                System.out.printf("%-12d | %-22s | %-12s | %-12s%n",
                        rs.getLong("customer_id"),
                        rs.getString("name"),
                        rs.getString("mobile"),
                        rs.getDate("date_of_birth").toString());
            }
            System.out.println("-----------------------------------------------------------------------------------");
            System.out.println("Total Customers without Disbursement: " + count);
        }
    }

    /**
     * Report 6: Repeat Applicants
     * Customers with > 1 application using GROUP BY and HAVING.
     */
    public void printRepeatApplicants() throws SQLException {
        String sql = "SELECT c.customer_id, c.name, c.mobile, " +
                     "       COUNT(a.application_id) AS app_count, " +
                     "       COALESCE(SUM(a.requested_amount), 0.00) AS total_requested_amount " +
                     "FROM customer c " +
                     "JOIN loan_application a ON c.customer_id = a.customer_id " +
                     "GROUP BY c.customer_id, c.name, c.mobile " +
                     "HAVING COUNT(a.application_id) > 1 " +
                     "ORDER BY app_count DESC, total_requested_amount DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n===================================================================================");
            System.out.println(" REPORT 6: REPEAT APPLICANTS (> 1 Loan Application)");
            System.out.println("===================================================================================");
            System.out.printf("%-12s | %-20s | %-12s | %12s | %20s%n", "Customer ID", "Customer Name", "Mobile", "Total Apps", "Total Requested (INR)");
            System.out.println("-----------------------------------------------------------------------------------");

            int count = 0;
            while (rs.next()) {
                count++;
                System.out.printf("%-12d | %-20s | %-12s | %12d | %20s%n",
                        rs.getLong("customer_id"),
                        rs.getString("name"),
                        rs.getString("mobile"),
                        rs.getInt("app_count"),
                        FinancialCalculator.formatIndianCurrency(rs.getBigDecimal("total_requested_amount")));
            }
            System.out.println("-----------------------------------------------------------------------------------");
            System.out.println("Total Repeat Applicants Found: " + count);
        }
    }
}
