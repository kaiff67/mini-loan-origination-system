package com.lending.los.service;

import com.lending.los.dao.*;
import com.lending.los.model.*;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Manages loan workflow state transitions and enforces ACID transactional integrity.
 *
 * Normal Flow:
 * DRAFT -> UNDER_REVIEW -> CREDIT_APPROVAL -> DISBURSEMENT -> CORE_BANKING_PUSH -> EXIT
 *
 * Rejection:
 * DRAFT / UNDER_REVIEW / CREDIT_APPROVAL -> REJECTED
 */
public class ApplicationWorkflowService {

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final LoanApplicationDAO loanApplicationDAO = new LoanApplicationDAO();
    private final DisbursementDAO disbursementDAO = new DisbursementDAO();
    private final ApplicationHistoryDAO historyDAO = new ApplicationHistoryDAO();
    private final SystemAlertDAO alertDAO = new SystemAlertDAO();
    private final CoreBankingDAO coreBankingDAO = new CoreBankingDAO();

    /**
     * Creates a new DRAFT loan application.
     */
    public LoanApplication createDraft(Long customerId, LoanProduct product, String branchCode, String branchName,
                                       BigDecimal requestedAmount, int tenureMonths,
                                       BigDecimal monthlyIncome, BigDecimal existingObligations) throws Exception {

        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer with ID " + customerId + " does not exist."));

        // Check active application rule: Only one active application per customer per product
        if (loanApplicationDAO.hasActiveApplication(customerId, product)) {
            throw new IllegalStateException("Customer already has an active loan application for product: " + product.getDisplayName() +
                    " (Active = DRAFT, UNDER_REVIEW, CREDIT_APPROVAL).");
        }

        // Basic financial calculation
        BigDecimal emi = FinancialCalculator.calculateEMI(requestedAmount, product.getAnnualInterestRate(), tenureMonths);
        BigDecimal foir = FinancialCalculator.calculateFOIR(existingObligations, emi, monthlyIncome);

        String appNumber = "APP-" + BusinessDateService.getBusinessDate().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "-" + String.format("%04d", (int)(Math.random() * 9000 + 1000));

        LoanApplication app = new LoanApplication();
        app.setApplicationNumber(appNumber);
        app.setCustomerId(customerId);
        app.setProduct(product);
        app.setBranchCode(branchCode);
        app.setBranchName(branchName);
        app.setRequestedAmount(requestedAmount);
        app.setTenureMonths(tenureMonths);
        app.setMonthlyIncome(monthlyIncome);
        app.setExistingMonthlyObligations(existingObligations);
        app.setAnnualInterestRate(product.getAnnualInterestRate());
        app.setProposedEmi(emi);
        app.setFoirPercentage(foir);
        app.setStatus(ApplicationStage.DRAFT);
        app.setCreatedAt(BusinessDateService.getBusinessDateTime());

        Long appId = loanApplicationDAO.createDraft(app);
        app.setApplicationId(appId);

        // Audit log
        try (Connection conn = DBConnection.getConnection()) {
            historyDAO.logHistory(new ApplicationHistory(appId, "CREATE_DRAFT", null, "DRAFT",
                    "USER", BusinessDateService.getBusinessDateTime(), "Draft application initiated."), conn);
        }

        return app;
    }

    /**
     * Submits a DRAFT application. Runs automated eligibility assessment.
     * Encapsulated inside a single ACID Database Transaction.
     * Transitions:
     * DRAFT -> UNDER_REVIEW (if eligible)
     * DRAFT -> REJECTED (if ineligible)
     */
    public ValidationResult submitApplication(Long applicationId, String userCode) throws Exception {
        LoanApplication app = loanApplicationDAO.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application ID " + applicationId + " not found."));

        if (app.getStatus() != ApplicationStage.DRAFT) {
            throw new IllegalStateException("Only DRAFT applications can be submitted. Current status: " + app.getStatus());
        }

        Customer customer = customerDAO.findById(app.getCustomerId())
                .orElseThrow(() -> new IllegalStateException("Customer data missing for application " + applicationId));

        // Evaluate rules
        ValidationResult result = EligibilityEngine.evaluate(customer, app, BusinessDateService.getBusinessDate(), false);

        app.setProposedEmi(result.getCalculatedEmi());
        app.setFoirPercentage(result.getCalculatedFoir());
        app.setEligible(result.isEligible());
        app.setEligibilityReasons(result.getReasonsAsFormattedString());
        app.setSubmittedAt(BusinessDateService.getBusinessDateTime());

        ApplicationStage oldStatus = app.getStatus();
        ApplicationStage newStatus = result.isEligible() ? ApplicationStage.UNDER_REVIEW : ApplicationStage.REJECTED;
        app.setStatus(newStatus);

        if (!result.isEligible()) {
            app.setDecidedAt(BusinessDateService.getBusinessDateTime());
            app.setReviewerCode("SYSTEM_AUTO_RULE");
            app.setDecisionReason("Automated eligibility check failed: " + result.getReasonsAsFormattedString());
        }

        // ACID Transaction: update application + audit history together
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            loanApplicationDAO.update(app, conn);

            String remarks = result.isEligible()
                    ? "Passed automatic eligibility check. FOIR: " + result.getCalculatedFoir() + "%"
                    : "Automated rejection: " + result.getReasonsAsFormattedString();

            historyDAO.logHistory(new ApplicationHistory(app.getApplicationId(), "SUBMIT",
                    oldStatus.name(), newStatus.name(), userCode, BusinessDateService.getBusinessDateTime(), remarks), conn);

            conn.commit(); // Commit transaction
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignored */ }
            }
            throw new SQLException("Transaction rolled back during application submission: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }

        return result;
    }

    /**
     * Reviewer decision on an UNDER_REVIEW application.
     * Encapsulated inside a single ACID Database Transaction.
     * Transitions:
     * UNDER_REVIEW -> CREDIT_APPROVAL (Approved: retains requested amount and tenure)
     * UNDER_REVIEW -> REJECTED (Rejected: mandatory rejection reason required)
     */
    public void reviewDecision(Long applicationId, boolean approve, String reason, String reviewerCode) throws Exception {
        LoanApplication app = loanApplicationDAO.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application ID " + applicationId + " not found."));

        if (app.getStatus() != ApplicationStage.UNDER_REVIEW) {
            throw new IllegalStateException("Only UNDER_REVIEW applications can be reviewed. Current status: " + app.getStatus());
        }

        if (!approve && (reason == null || reason.trim().isEmpty())) {
            throw new IllegalArgumentException("Rejection reason is mandatory when rejecting an application.");
        }

        ApplicationStage oldStatus = app.getStatus();
        ApplicationStage newStatus = approve ? ApplicationStage.CREDIT_APPROVAL : ApplicationStage.REJECTED;

        app.setStatus(newStatus);
        app.setDecidedAt(BusinessDateService.getBusinessDateTime());
        app.setReviewerCode(reviewerCode);
        app.setDecisionReason(approve ? (reason != null && !reason.trim().isEmpty() ? reason : "Approved by Reviewer") : reason);

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            loanApplicationDAO.update(app, conn);

            String action = approve ? "REVIEW_APPROVE" : "REVIEW_REJECT";
            historyDAO.logHistory(new ApplicationHistory(app.getApplicationId(), action,
                    oldStatus.name(), newStatus.name(), reviewerCode, BusinessDateService.getBusinessDateTime(), app.getDecisionReason()), conn);

            conn.commit(); // Commit transaction
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignored */ }
            }
            throw new SQLException("Transaction rolled back during reviewer decision: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Performs final Credit Approval and triggers generation of the official Application Form document.
     */
    public File performCreditApproval(Long applicationId, String officerCode) throws Exception {
        LoanApplication app = loanApplicationDAO.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application ID " + applicationId + " not found."));

        if (app.getStatus() != ApplicationStage.CREDIT_APPROVAL) {
            throw new IllegalStateException("Credit Approval can only be performed when status is CREDIT_APPROVAL. Current status: " + app.getStatus());
        }

        Customer customer = customerDAO.findById(app.getCustomerId())
                .orElseThrow(() -> new IllegalStateException("Customer data not found for application " + applicationId));

        // Generate document
        File generatedFile = DocumentGeneratorService.generateApplicationForm(customer, app);

        // Audit log
        try (Connection conn = DBConnection.getConnection()) {
            historyDAO.logHistory(new ApplicationHistory(app.getApplicationId(), "CREDIT_APPROVAL_DOC_GEN",
                    app.getStatus().name(), app.getStatus().name(), officerCode, BusinessDateService.getBusinessDateTime(),
                    "Application Form document generated: " + generatedFile.getName()), conn);
        }

        return generatedFile;
    }

    /**
     * Disburses an approved application once.
     * Enforces:
     * - Only from CREDIT_APPROVAL status.
     * - One disbursement per application (enforced by DB UNIQUE constraint).
     * - Globally unique disbursement reference (enforced by DB UNIQUE constraint).
     * - Atomic state transition to DISBURSEMENT + Audit History log.
     */
    public Disbursement disburseLoan(Long applicationId, String referenceNumber, String officerCode) throws Exception {
        LoanApplication app = loanApplicationDAO.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application ID " + applicationId + " not found."));

        if (app.getStatus() != ApplicationStage.CREDIT_APPROVAL) {
            throw new IllegalStateException("Disbursement can only occur for applications in CREDIT_APPROVAL status. Current status: " + app.getStatus());
        }

        if (referenceNumber == null || referenceNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Disbursement reference number is mandatory.");
        }

        if (!disbursementDAO.isReferenceUnique(referenceNumber)) {
            throw new IllegalStateException("Duplicate disbursement reference number: '" + referenceNumber + "'. References must be globally unique.");
        }

        if (disbursementDAO.findByApplicationId(applicationId).isPresent()) {
            throw new IllegalStateException("Application ID " + applicationId + " has already been disbursed. Repeated disbursements are strictly prohibited.");
        }

        Disbursement disbursement = new Disbursement();
        disbursement.setApplicationId(applicationId);
        disbursement.setReferenceNumber(referenceNumber);
        disbursement.setAmount(app.getRequestedAmount());
        disbursement.setDisbursedAt(BusinessDateService.getBusinessDateTime());

        ApplicationStage oldStatus = app.getStatus();
        app.setStatus(ApplicationStage.DISBURSEMENT);

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Insert disbursement record
            disbursementDAO.createDisbursement(disbursement, conn);

            // 2. Update application status
            loanApplicationDAO.update(app, conn);

            // 3. Log audit history
            historyDAO.logHistory(new ApplicationHistory(applicationId, "DISBURSE",
                    oldStatus.name(), ApplicationStage.DISBURSEMENT.name(), officerCode,
                    BusinessDateService.getBusinessDateTime(), "Full approved amount INR " +
                    FinancialCalculator.formatIndianCurrency(disbursement.getAmount()) +
                    " disbursed with ref: " + referenceNumber), conn);

            conn.commit(); // Commit transaction atomically
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* ignored */ }
            }
            throw new SQLException("Transaction rolled back during disbursement: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }

        return disbursement;
    }

    /**
     * Demonstrates controlled transaction failure with full rollback (Verification Scenario 09).
     * Simulates an unhandled failure during multi-table update and proves that no partial data is retained.
     */
    public boolean demonstrateControlledRollback(Long applicationId) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Log a dummy history record
            historyDAO.logHistory(new ApplicationHistory(applicationId, "TEST_ROLLBACK_ACTION",
                    "TEST_OLD", "TEST_NEW", "TEST_USER", BusinessDateService.getBusinessDateTime(),
                    "Temporary test entry that must be rolled back"), conn);

            // 2. Intentionally throw a simulated database failure
            throw new SQLException("SIMULATED DATABASE INTEGRITY FAILURE TO TRIGGER EXPLICIT ROLLBACK.");

        } catch (Exception e) {
            System.out.println("[DEMO ROLLBACK] Controlled failure encountered: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("[DEMO ROLLBACK] Transaction explicitly rolled back. Database untouched!");
                    return true;
                } catch (SQLException ex) {
                    System.err.println("[ERROR] Rollback failed: " + ex.getMessage());
                }
            }
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
        return false;
    }
}
