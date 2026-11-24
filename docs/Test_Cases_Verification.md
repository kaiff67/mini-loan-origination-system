# Test Case Verification Document
## MINI LOAN ORIGINATION SYSTEM (LOS)

This document contains the complete test verification matrix and boundary validation scenarios for the Mini Loan Origination System (LOS).

---

### Verification Scenarios Matrix

| Scenario # | Test Scenario Title | Input Data / Preconditions | Expected Behavior | Verification Status |
| :---: | :--- | :--- | :--- | :---: |
| **01** | Product Min/Max Boundaries | PERSONAL: Amount ₹40,000 (below min ₹50,000) or ₹6,00,000 (above max ₹5,00,000). Tenure: 10 months (below 12) or 65 months (above 60). | Rejected by `EligibilityEngine` with exact boundary failure message. | ✅ PASSED |
| **02** | Customer Age Boundaries (20, 21, 60, 61) | Current Business Date: `2026-10-23`<br>• Age 20 (`DOB: 2006-10-24`)<br>• Age 21 (`DOB: 2005-10-23`)<br>• Age 60 (`DOB: 1966-10-23`)<br>• Age 61 (`DOB: 1965-10-22`) | • Age 20: Rejected ❌<br>• Age 21: Accepted ✅<br>• Age 60: Accepted ✅<br>• Age 61: Rejected ❌ | ✅ PASSED |
| **03** | Duplicate Mobile & Active Application | Customer 1 has an active application (status = `UNDER_REVIEW`). Customer tries to apply for another `PERSONAL` loan or register with duplicate mobile. | System rejects new application with message: *"Customer already has an active application for Personal Loan"*. Database rejects duplicate mobile via `UNIQUE` constraint. | ✅ PASSED |
| **04** | Worked EMI Calculation Example | • Product: `PERSONAL` (Rate = 12% p.a.)<br>• Principal: ₹1,00,000.00<br>• Tenure: 12 months | EMI calculates to exactly **INR 8,884.88** using `FinancialCalculator.calculateEMI` matching formula $P \cdot r \cdot \frac{(1+r)^n}{(1+r)^n - 1}$. | ✅ PASSED |
| **05** | Affordability (FOIR) Threshold (50% vs above) | • Case A: Income ₹30,000, Debts ₹5,000, EMI ₹8,884.88 $\rightarrow$ FOIR = **46.28%**<br>• Case B: Income ₹30,000, Debts ₹7,000, EMI ₹8,884.88 $\rightarrow$ FOIR = **52.95%** | • Case A: Eligible ($\le 50\%$), transitions to `UNDER_REVIEW` ✅<br>• Case B: Ineligible ($> 50\%$), transitions directly to `REJECTED` ❌ | ✅ PASSED |
| **06** | Invalid State Transitions Rejected | Application in `DRAFT` status attempted to be directly disbursed, or `UNDER_REVIEW` attempted to be disbursed without credit approval. | Throws `IllegalStateException: Only CREDIT_APPROVAL applications can be disbursed. Current status: DRAFT`. State remains unchanged. | ✅ PASSED |
| **07** | Generated Application Form Document | Application passes reviewer check and reaches `CREDIT_APPROVAL`. Option 6 executed. | Formatted official text document `Application_Form_APP-XXXX.txt` generated in `generated_documents/` with applicant, loan, and approval signatures. | ✅ PASSED |
| **08** | Duplicate & Repeated Disbursement Attempts | Attempting to disburse an already disbursed application (ID 4) or reusing reference `DISB-20261021-998877`. | Database throws `SQLIntegrityConstraintViolationException` on unique key constraint; service prevents action before committing. | ✅ PASSED |
| **09** | Controlled Transaction Failure with Full Rollback | Execute Option 10 ➔ 3 (simulates integrity failure mid-transaction). | Audit log entry is rolled back; database connection auto-rollback verified. No partial records persisted in tables. | ✅ PASSED |
| **10** | Review Ages of Exactly 2 and 3 Days | Application submitted on `2026-10-23`.<br>• Advance Business Date to `2026-10-25` (2 days elapsed).<br>• Advance Business Date to `2026-10-26` (3 days elapsed). | • At 2 days: Not overdue (Rule: $> 2$ calendar days).<br>• At 3 days: Flagged as `REVIEW_OVERDUE` in utility and CSV. | ✅ PASSED |
| **11** | Repeat Utility Execution without Duplicate Alerts | Run Overdue Review Utility twice in succession on the same business date. | Utility logs: `New Alerts Created: 0` on second run. `SystemAlertDAO` and DB unique constraint prevent duplicate records. | ✅ PASSED |
| **12** | Persistence After Application Restart | Stop the Java application and restart `Main.java`. | All customers, applications, disbursements, and history records persist intact in MySQL InnoDB tables. | ✅ PASSED |

---

### Step-by-Step Demo Walkthrough Guide for Examiners

When your examiner asks you to demonstrate any of these scenarios:

1. **To demo Scenario 04 & 05 (Worked EMI & FOIR):**
   * Select Option `3` (Create DRAFT).
   * Enter: Customer `1`, Product `1` (PERSONAL), Amount `100000`, Tenure `12`, Income `30000`, Obligations `5000`.
   * Select Option `4` (Submit). Show that EMI is `8,884.88` and FOIR is `46.28%` (Passed).
   * Repeat with Obligations `7000`. Show that FOIR is `52.95%` and status is automatically `REJECTED`.

2. **To demo Scenario 02 (Age Boundaries):**
   * Register customer with DOB `2006-10-24` (Age 20) $\rightarrow$ Submission fails.
   * Register customer with DOB `2005-10-23` (Age 21) $\rightarrow$ Submission succeeds.

3. **To demo Scenario 10 & 11 (Overdue Utility & Duplicate Safety):**
   * Select Option `10 ➔ 2` (Set Business Date to `2026-10-26`).
   * Select Option `9 ➔ 1` (Run Overdue Utility Now).
   * Notice that `APP-20261023-0001` is flagged as Overdue and exported to CSV!
   * Run Option `9 ➔ 1` a second time immediately. Notice: `New Alerts Created: 0` (Idempotent safe!).

4. **To demo Scenario 09 (Transaction Rollback):**
   * Select Option `10 ➔ 3`.
   * The console will show: `[DEMO ROLLBACK] Controlled failure encountered: SIMULATED DATABASE INTEGRITY FAILURE TO TRIGGER EXPLICIT ROLLBACK. Transaction explicitly rolled back.`
