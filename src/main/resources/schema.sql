-- ============================================================================
-- Enterprise Lending Application Mini Loan Origination Application (LOS)
-- Database: MySQL 8.0+
-- File: schema.sql
-- ============================================================================

DROP DATABASE IF EXISTS loan_origination_db;
CREATE DATABASE loan_origination_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE loan_origination_db;

-- ----------------------------------------------------------------------------
-- Table 1: Customer
-- Stores demographic, KYC, and contact details of loan applicants.
-- Enforces: unique mobile (10 digits), positive age calculation in app.
-- ----------------------------------------------------------------------------
CREATE TABLE customer (
    customer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    mobile VARCHAR(10) NOT NULL UNIQUE,
    address VARCHAR(255) NOT NULL,
    kyc_details VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_mobile_length CHECK (CHAR_LENGTH(mobile) = 10)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table 2: Loan Application
-- Manages loan lifecycle, financial inputs, credit assessments, and status.
-- ----------------------------------------------------------------------------
CREATE TABLE loan_application (
    application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_number VARCHAR(30) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    product VARCHAR(20) NOT NULL,
    branch_code VARCHAR(20) NOT NULL,
    branch_name VARCHAR(100) NOT NULL,
    requested_amount DECIMAL(15, 2) NOT NULL,
    tenure_months INT NOT NULL,
    monthly_income DECIMAL(15, 2) NOT NULL,
    existing_monthly_obligations DECIMAL(15, 2) NOT NULL,
    annual_interest_rate DECIMAL(5, 2) NOT NULL,
    proposed_emi DECIMAL(15, 2) NOT NULL,
    foir_percentage DECIMAL(5, 2) NOT NULL,
    eligible BOOLEAN NOT NULL DEFAULT FALSE,
    eligibility_reasons TEXT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    submitted_at TIMESTAMP NULL,
    decided_at TIMESTAMP NULL,
    reviewer_code VARCHAR(50) NULL,
    decision_reason TEXT NULL,
    CONSTRAINT fk_loan_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id) ON DELETE RESTRICT,
    CONSTRAINT chk_product_type CHECK (product IN ('PERSONAL', 'EDUCATION')),
    CONSTRAINT chk_status_valid CHECK (status IN ('DRAFT', 'UNDER_REVIEW', 'CREDIT_APPROVAL', 'DISBURSEMENT', 'CORE_BANKING_PUSH', 'EXIT', 'REJECTED')),
    CONSTRAINT chk_positive_income CHECK (monthly_income > 0),
    CONSTRAINT chk_non_negative_obligations CHECK (existing_monthly_obligations >= 0),
    CONSTRAINT chk_positive_tenure CHECK (tenure_months > 0)
) ENGINE=InnoDB;

-- Index for checking active application per customer per product
CREATE INDEX idx_customer_product_status ON loan_application(customer_id, product, status);
CREATE INDEX idx_app_status ON loan_application(status);
CREATE INDEX idx_app_submitted_at ON loan_application(submitted_at);

-- ----------------------------------------------------------------------------
-- Table 3: Disbursement
-- Enforces: exactly one disbursement per application (UNIQUE application_id),
-- and globally unique disbursement reference number.
-- ----------------------------------------------------------------------------
CREATE TABLE disbursement (
    disbursement_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL UNIQUE,
    reference_number VARCHAR(50) NOT NULL UNIQUE,
    amount DECIMAL(15, 2) NOT NULL,
    disbursed_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_disbursement_app FOREIGN KEY (application_id) REFERENCES loan_application(application_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table 4: Application History (Audit Trail)
-- Captures state transitions, actor, timestamp, and audit remarks.
-- ----------------------------------------------------------------------------
CREATE TABLE application_history (
    history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    old_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NOT NULL,
    performed_by VARCHAR(50) NOT NULL,
    performed_at TIMESTAMP NOT NULL,
    remarks TEXT NULL,
    CONSTRAINT fk_history_app FOREIGN KEY (application_id) REFERENCES loan_application(application_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_history_app_id ON application_history(application_id);

-- ----------------------------------------------------------------------------
-- Table 5: System Alerts
-- Idempotent alert table. Prevents duplicate alerts per application using
-- UNIQUE KEY on (application_id, alert_type).
-- ----------------------------------------------------------------------------
CREATE TABLE system_alerts (
    alert_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_alert_app FOREIGN KEY (application_id) REFERENCES loan_application(application_id) ON DELETE CASCADE,
    CONSTRAINT uk_application_alert_type UNIQUE (application_id, alert_type)
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table 6: Utility Run
-- Tracks execution runs of background utilities (Overdue Review, CoreDataPush).
-- ----------------------------------------------------------------------------
CREATE TABLE utility_run (
    run_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    utility_name VARCHAR(50) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NULL,
    result VARCHAR(20) NOT NULL,
    applications_processed INT DEFAULT 0,
    alerts_created INT DEFAULT 0,
    error_message TEXT NULL
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Table 7: NG_CORE_BANKING
-- Represents external Core Banking System table populated post-disbursement.
-- Enforces: unique ApplicationID to guarantee no duplicate pushes.
-- ----------------------------------------------------------------------------
CREATE TABLE ng_core_banking (
    core_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    CustomerID BIGINT NOT NULL,
    ApplicationID BIGINT NOT NULL UNIQUE,
    Customer_Name VARCHAR(100) NOT NULL,
    Address VARCHAR(255) NOT NULL,
    DOB DATE NOT NULL,
    KYC_Details VARCHAR(100) NOT NULL,
    Approved_Amount DECIMAL(15, 2) NOT NULL,
    Disbursed_Amount DECIMAL(15, 2) NOT NULL,
    Disbursed_Date TIMESTAMP NOT NULL
) ENGINE=InnoDB;
