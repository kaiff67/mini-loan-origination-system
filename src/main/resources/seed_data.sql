-- ============================================================================
-- Enterprise Lending Application Mini Loan Origination Application (LOS)
-- Seed Data Script: seed_data.sql
-- ============================================================================

USE loan_origination_db;

-- Clean existing data in reverse order of foreign keys
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE ng_core_banking;
TRUNCATE TABLE utility_run;
TRUNCATE TABLE system_alerts;
TRUNCATE TABLE application_history;
TRUNCATE TABLE disbursement;
TRUNCATE TABLE loan_application;
TRUNCATE TABLE customer;
SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- 1. Insert Sample Customers
-- ----------------------------------------------------------------------------
-- Customer 1: Age 28 (Passes age 21-60 rule)
INSERT INTO customer (customer_id, name, date_of_birth, mobile, address, kyc_details, created_at)
VALUES (1, 'Aarav Sharma', '1998-05-15', '9876543210', 'A-42, Sector 62, Noida, UP', 'AADHAAR-8921-3412-9012', '2026-10-20 10:00:00');

-- Customer 2: Age 35 (Passes age 21-60 rule)
INSERT INTO customer (customer_id, name, date_of_birth, mobile, address, kyc_details, created_at)
VALUES (2, 'Priya Patel', '1991-08-22', '9823456789', 'B-104, SG Highway, Ahmedabad, Gujarat', 'PAN-ABCDE1234F', '2026-10-21 11:30:00');

-- Customer 3: Age 24 (Passes age 21-60 rule, Education loan applicant)
INSERT INTO customer (customer_id, name, date_of_birth, mobile, address, kyc_details, created_at)
VALUES (3, 'Rohan Verma', '2002-03-10', '9811223344', 'C-12, Indiranagar, Bengaluru, Karnataka', 'AADHAAR-4412-5566-7788', '2026-10-22 09:15:00');

-- Customer 4: Age 45 (Passes age 21-60 rule, Repeat applicant candidate)
INSERT INTO customer (customer_id, name, date_of_birth, mobile, address, kyc_details, created_at)
VALUES (4, 'Vikram Malhotra', '1981-11-05', '9988776655', 'Plot 5, Hitech City, Hyderabad, Telangana', 'PAN-XYZPQ9876K', '2026-10-18 14:00:00');

-- Customer 5: Age 52 (For customer without disbursement scenario)
INSERT INTO customer (customer_id, name, date_of_birth, mobile, address, kyc_details, created_at)
VALUES (5, 'Sunita Rao', '1974-04-18', '9712345678', 'Flat 302, Shivaji Nagar, Pune, Maharashtra', 'AADHAAR-9900-1122-3344', '2026-10-19 16:45:00');

-- ----------------------------------------------------------------------------
-- 2. Insert Sample Loan Applications across Lifecycle Stages
-- ----------------------------------------------------------------------------

-- App 1: Customer 1 -> Submitted 3 days ago on 2026-10-23 (To demonstrate OVERDUE REVIEW (> 2 days waiting) when business date is 2026-10-26)
INSERT INTO loan_application (
    application_id, application_number, customer_id, product, branch_code, branch_name,
    requested_amount, tenure_months, monthly_income, existing_monthly_obligations,
    annual_interest_rate, proposed_emi, foir_percentage, eligible, eligibility_reasons,
    status, created_at, submitted_at
) VALUES (
    1, 'APP-20261023-0001', 1, 'PERSONAL', 'BR-DEL-01', 'Connaught Place Branch',
    100000.00, 12, 30000.00, 5000.00,
    12.00, 8884.88, 46.28, TRUE, 'All criteria met successfully.',
    'UNDER_REVIEW', '2026-10-23 09:30:00', '2026-10-23 10:00:00'
);

-- App 2: Customer 2 -> Education Loan in CREDIT_APPROVAL stage
INSERT INTO loan_application (
    application_id, application_number, customer_id, product, branch_code, branch_name,
    requested_amount, tenure_months, monthly_income, existing_monthly_obligations,
    annual_interest_rate, proposed_emi, foir_percentage, eligible, eligibility_reasons,
    status, created_at, submitted_at, decided_at, reviewer_code, decision_reason
) VALUES (
    2, 'APP-20261024-0002', 2, 'EDUCATION', 'BR-AHM-01', 'Navrangpura Branch',
    300000.00, 36, 45000.00, 6000.00,
    10.00, 9680.16, 34.84, TRUE, 'Underwritten and approved by credit reviewer.',
    'CREDIT_APPROVAL', '2026-10-24 11:00:00', '2026-10-24 11:30:00', '2026-10-24 15:00:00', 'REV_NEHA', 'Excellent creditworthiness and academic record.'
);

-- App 3: Customer 3 -> Fresh DRAFT stage application
INSERT INTO loan_application (
    application_id, application_number, customer_id, product, branch_code, branch_name,
    requested_amount, tenure_months, monthly_income, existing_monthly_obligations,
    annual_interest_rate, proposed_emi, foir_percentage, eligible, eligibility_reasons,
    status, created_at
) VALUES (
    3, 'APP-20261025-0003', 3, 'PERSONAL', 'BR-BLR-01', 'Indiranagar Branch',
    150000.00, 24, 35000.00, 4000.00,
    12.00, 7061.02, 31.60, TRUE, 'Draft created, pending submission.',
    'DRAFT', '2026-10-25 14:20:00'
);

-- App 4: Customer 4 -> Fully DISBURSED Personal Loan (Already moved to EXIT via CoreDataPush)
INSERT INTO loan_application (
    application_id, application_number, customer_id, product, branch_code, branch_name,
    requested_amount, tenure_months, monthly_income, existing_monthly_obligations,
    annual_interest_rate, proposed_emi, foir_percentage, eligible, eligibility_reasons,
    status, created_at, submitted_at, decided_at, reviewer_code, decision_reason
) VALUES (
    4, 'APP-20261020-0004', 4, 'PERSONAL', 'BR-HYD-01', 'Madhapur Branch',
    200000.00, 24, 60000.00, 8000.00,
    12.00, 9414.69, 29.02, TRUE, 'Eligible and approved.',
    'EXIT', '2026-10-20 09:00:00', '2026-10-20 09:30:00', '2026-10-20 14:00:00', 'REV_AMIT', 'Profile strong, instant approval.'
);

-- App 5: Customer 4 -> Second application by same customer (Education Loan) to demonstrate REPEAT APPLICANTS
INSERT INTO loan_application (
    application_id, application_number, customer_id, product, branch_code, branch_name,
    requested_amount, tenure_months, monthly_income, existing_monthly_obligations,
    annual_interest_rate, proposed_emi, foir_percentage, eligible, eligibility_reasons,
    status, created_at, submitted_at
) VALUES (
    5, 'APP-20261024-0005', 4, 'EDUCATION', 'BR-HYD-01', 'Madhapur Branch',
    500000.00, 48, 60000.00, 9414.69,
    10.00, 12681.29, 36.83, TRUE, 'Second product application is allowed after previous personal loan was disbursed.',
    'UNDER_REVIEW', '2026-10-24 16:00:00', '2026-10-24 16:30:00'
);

-- App 6: Customer 5 -> Ineligible application that was REJECTED at submission due to FOIR > 50%
INSERT INTO loan_application (
    application_id, application_number, customer_id, product, branch_code, branch_name,
    requested_amount, tenure_months, monthly_income, existing_monthly_obligations,
    annual_interest_rate, proposed_emi, foir_percentage, eligible, eligibility_reasons,
    status, created_at, submitted_at, decided_at, decision_reason
) VALUES (
    6, 'APP-20261022-0006', 5, 'PERSONAL', 'BR-PUN-01', 'Kothrud Branch',
    100000.00, 12, 30000.00, 7000.00,
    12.00, 8884.88, 52.95, FALSE, 'FOIR calculation 52.95% exceeds maximum allowed limit of 50.00%.',
    'REJECTED', '2026-10-22 10:15:00', '2026-10-22 10:20:00', '2026-10-22 10:20:00', 'Automated rule engine rejection on submission: FOIR > 50%.'
);

-- ----------------------------------------------------------------------------
-- 3. Insert Disbursement for App 4
-- ----------------------------------------------------------------------------
INSERT INTO disbursement (disbursement_id, application_id, reference_number, amount, disbursed_at)
VALUES (1, 4, 'DISB-20261021-998877', 200000.00, '2026-10-21 16:30:00');

-- ----------------------------------------------------------------------------
-- 4. Insert Core Banking Record for App 4 (Simulating post-push state)
-- ----------------------------------------------------------------------------
INSERT INTO ng_core_banking (
    core_id, CustomerID, ApplicationID, Customer_Name, Address, DOB, KYC_Details,
    Approved_Amount, Disbursed_Amount, Disbursed_Date
) VALUES (
    1, 4, 4, 'Vikram Malhotra', 'Plot 5, Hitech City, Hyderabad, Telangana',
    '1981-11-05', 'PAN-XYZPQ9876K', 200000.00, 200000.00, '2026-10-21 16:30:00'
);

-- ----------------------------------------------------------------------------
-- 5. Insert Application History Audit Records
-- ----------------------------------------------------------------------------
INSERT INTO application_history (application_id, action, old_status, new_status, performed_by, performed_at, remarks)
VALUES 
(1, 'CREATE_DRAFT', NULL, 'DRAFT', 'CUST_AARAV', '2026-10-23 09:30:00', 'Draft application initiated.'),
(1, 'SUBMIT', 'DRAFT', 'UNDER_REVIEW', 'CUST_AARAV', '2026-10-23 10:00:00', 'Eligibility passed with FOIR 46.28%.'),
(2, 'CREATE_DRAFT', NULL, 'DRAFT', 'CUST_PRIYA', '2026-10-24 11:00:00', 'Draft application initiated.'),
(2, 'SUBMIT', 'DRAFT', 'UNDER_REVIEW', 'CUST_PRIYA', '2026-10-24 11:30:00', 'Eligibility passed with FOIR 34.84%.'),
(2, 'REVIEW_APPROVE', 'UNDER_REVIEW', 'CREDIT_APPROVAL', 'REV_NEHA', '2026-10-24 15:00:00', 'Approved by credit officer Neha.'),
(4, 'CREATE_DRAFT', NULL, 'DRAFT', 'CUST_VIKRAM', '2026-10-20 09:00:00', 'Draft initiated.'),
(4, 'SUBMIT', 'DRAFT', 'UNDER_REVIEW', 'CUST_VIKRAM', '2026-10-20 09:30:00', 'Eligibility passed with FOIR 29.02%.'),
(4, 'REVIEW_APPROVE', 'UNDER_REVIEW', 'CREDIT_APPROVAL', 'REV_AMIT', '2026-10-20 14:00:00', 'Approved by reviewer Amit.'),
(4, 'DISBURSE', 'CREDIT_APPROVAL', 'DISBURSEMENT', 'DISB_OFFICER_1', '2026-10-21 16:30:00', 'Disbursement processed with ref DISB-20261021-998877.'),
(4, 'CORE_BANKING_PUSH', 'DISBURSEMENT', 'EXIT', 'SYSTEM_UTILITY', '2026-10-21 16:35:00', 'Pushed to Core Banking table successfully.');
