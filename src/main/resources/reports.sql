-- ============================================================================
-- Enterprise Lending Application Mini Loan Origination Application (LOS)
-- Six Required SQL Reports (reports.sql)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Report 1: Application Register
-- Required Output: Application number, customer name, product, amount, EMI,
--                  status, and submission date. Filter by product and status.
-- Parameters:
--   ? = product filter (or pass NULL / 'ALL' for no filter)
--   ? = status filter (or pass NULL / 'ALL' for no filter)
-- ----------------------------------------------------------------------------
-- 1.1 Parameterized Query
SELECT 
    a.application_number,
    c.name AS customer_name,
    a.product,
    a.requested_amount,
    a.proposed_emi,
    a.status,
    a.submitted_at
FROM loan_application a
JOIN customer c ON a.customer_id = c.customer_id
WHERE (? IS NULL OR ? = 'ALL' OR a.product = ?)
  AND (? IS NULL OR ? = 'ALL' OR a.status = ?)
ORDER BY a.created_at DESC;

-- ----------------------------------------------------------------------------
-- Report 2: Product Pipeline
-- Required Output:
--   2A. Application count and sum of requested amounts grouped by product and status.
--   2B. Application count and sum of requested amounts grouped by branch and product.
-- ----------------------------------------------------------------------------
-- 2A. Grouped by Product and Status
SELECT 
    a.product,
    a.status,
    COUNT(a.application_id) AS application_count,
    COALESCE(SUM(a.requested_amount), 0.00) AS total_requested_amount
FROM loan_application a
GROUP BY a.product, a.status
ORDER BY a.product, a.status;

-- 2B. Grouped by Branch and Product
SELECT 
    a.branch_code,
    a.branch_name,
    a.product,
    COUNT(a.application_id) AS application_count,
    COALESCE(SUM(a.requested_amount), 0.00) AS total_requested_amount
FROM loan_application a
GROUP BY a.branch_code, a.branch_name, a.product
ORDER BY a.branch_code, a.product;

-- ----------------------------------------------------------------------------
-- Report 3: Daily / Monthly / Quarterly Disbursement
-- Required Output: Date, count and total amount for a supplied date range.
--                  Branch-wise reports too.
-- Parameters:
--   ? = Start Date (e.g. '2026-10-01 00:00:00')
--   ? = End Date   (e.g. '2026-10-31 23:59:59')
-- ----------------------------------------------------------------------------
-- 3A. Date-wise aggregate
SELECT 
    DATE(d.disbursed_at) AS disbursement_date,
    COUNT(d.disbursement_id) AS disbursement_count,
    COALESCE(SUM(d.amount), 0.00) AS total_disbursed_amount
FROM disbursement d
WHERE d.disbursed_at BETWEEN ? AND ?
GROUP BY DATE(d.disbursed_at)
ORDER BY disbursement_date ASC;

-- 3B. Branch-wise aggregate
SELECT 
    a.branch_code,
    a.branch_name,
    COUNT(d.disbursement_id) AS disbursement_count,
    COALESCE(SUM(d.amount), 0.00) AS total_disbursed_amount
FROM disbursement d
JOIN loan_application a ON d.application_id = a.application_id
WHERE d.disbursed_at BETWEEN ? AND ?
GROUP BY a.branch_code, a.branch_name
ORDER BY a.branch_code ASC;

-- ----------------------------------------------------------------------------
-- Report 4: Overdue Reviews
-- Required Output: Current UNDER_REVIEW applications older than 2 calendar days,
--                  including customer, amount, and days waiting.
-- Parameters:
--   ? = Current Business Date (e.g. '2026-10-26')
-- ----------------------------------------------------------------------------
SELECT 
    a.application_number,
    c.name AS customer_name,
    c.mobile,
    a.requested_amount,
    a.submitted_at,
    DATEDIFF(?, DATE(a.submitted_at)) AS days_waiting
FROM loan_application a
JOIN customer c ON a.customer_id = c.customer_id
WHERE a.status = 'UNDER_REVIEW'
  AND DATEDIFF(?, DATE(a.submitted_at)) > 2
ORDER BY days_waiting DESC;

-- ----------------------------------------------------------------------------
-- Report 5: Customers without Disbursement
-- Required Output: Customers with at least one application and no disbursement
--                  across any application. Return each customer once.
-- ----------------------------------------------------------------------------
SELECT DISTINCT 
    c.customer_id,
    c.name AS customer_name,
    c.mobile,
    c.date_of_birth
FROM customer c
JOIN loan_application a ON c.customer_id = a.customer_id
WHERE c.customer_id NOT IN (
    SELECT DISTINCT app.customer_id
    FROM loan_application app
    JOIN disbursement d ON app.application_id = d.application_id
)
ORDER BY c.customer_id ASC;

-- ----------------------------------------------------------------------------
-- Report 6: Repeat Applicants
-- Required Output: Customers with more than one application, including
--                  application count and total requested amount.
-- Uses: GROUP BY and HAVING.
-- ----------------------------------------------------------------------------
SELECT 
    c.customer_id,
    c.name AS customer_name,
    c.mobile,
    COUNT(a.application_id) AS application_count,
    COALESCE(SUM(a.requested_amount), 0.00) AS total_requested_amount
FROM customer c
JOIN loan_application a ON c.customer_id = a.customer_id
GROUP BY c.customer_id, c.name, c.mobile
HAVING COUNT(a.application_id) > 1
ORDER BY application_count DESC, total_requested_amount DESC;
