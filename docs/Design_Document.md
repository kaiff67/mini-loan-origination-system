# Architecture & Technical Design Document
## Mini Loan Origination System (LOS)
**Author:** Mohd Kaif Ahmad  
**Role:** Software Engineer  
**Tech Stack:** Java 17+, Pure JDBC, MySQL 8.0+

---

## 1. Executive Summary
The **Mini Loan Origination System (LOS)** is an enterprise backend application engineered to manage the end-to-end lifecycle of retail credit products (Personal and Education loans). The system features an automated underwriting engine (calculating Equated Monthly Installment [EMI] and Fixed Obligation to Income Ratio [FOIR]), multi-tier approval workflows, strict ACID transaction boundaries, asynchronous background processing, and core banking integration.

---

## 2. Architectural Design & Package Layout

The application strictly adheres to a **Layered Architecture (Separation of Concerns)**:

```
mini-loan-origination/
├── src/main/java/com/lending/los/
│   ├── Main.java                        # System bootstrap & thread pool initialization
│   ├── model/                           # Domain Entities & Business Enums
│   │   ├── Customer.java
│   │   ├── LoanApplication.java
│   │   ├── LoanProduct.java
│   │   ├── ApplicationStage.java
│   │   ├── Disbursement.java
│   │   ├── ApplicationHistory.java
│   │   ├── SystemAlert.java
│   │   └── CoreBankingRecord.java
│   ├── dao/                             # Pure JDBC Data Access Objects & SQL Queries
│   │   ├── DBConnection.java
│   │   ├── CustomerDAO.java
│   │   ├── LoanApplicationDAO.java
│   │   ├── DisbursementDAO.java
│   │   ├── ApplicationHistoryDAO.java
│   │   ├── SystemAlertDAO.java
│   │   ├── CoreBankingDAO.java
│   │   └── ReportDAO.java
│   ├── service/                         # Business Domain Logic, Validation, Math & Transactions
│   │   ├── BusinessDateService.java
│   │   ├── FinancialCalculator.java
│   │   ├── EligibilityEngine.java
│   │   ├── ValidationResult.java
│   │   ├── DocumentGeneratorService.java
│   │   └── ApplicationWorkflowService.java
│   ├── utility/                         # Scheduled & Background Executors
│   │   ├── OverdueReviewUtility.java
│   │   └── CoreDataPushUtility.java
│   └── ui/                              # Presentation Layer (Console CLI)
│       └── ConsoleMenu.java
└── src/main/resources/
    ├── db.properties                    # Database connection properties
    ├── schema.sql                       # DDL schema definition & constraints
    ├── seed_data.sql                    # Initial seed data for test scenarios
    └── reports.sql                      # Six analytical parameterized queries
```

---

## 3. Workflow State Machine

The loan lifecycle is governed by a finite state machine preventing unauthorized state jumps:

```
[DRAFT] ──(submit: pass)──► [UNDER_REVIEW] ──(approve)──► [CREDIT_APPROVAL] ──(disburse)──► [DISBURSEMENT] ──(core push)──► [EXIT]
   │                              │                              │
   └──(submit: fail)──────────────┴───────(reject with reason)───┴─────────────────────────────────────────► [REJECTED]
```

* **DRAFT:** Data entry and modifications permitted.
* **UNDER_REVIEW:** Automated checks passed; under credit officer examination.
* **CREDIT_APPROVAL:** Reviewer approval completed; Application Form document generated.
* **DISBURSEMENT:** Funds released atomically with unique transaction reference.
* **CORE_BANKING_PUSH:** System stage where background utility pushes records to `NG_CORE_BANKING`.
* **EXIT / REJECTED:** Immutable terminal states.

---

## 4. Financial Calculation Engine

### 4.1 Equated Monthly Installment (EMI) Formula
Calculated using banking amortization formula:
$$\text{EMI} = P \times r \times \frac{(1+r)^n}{(1+r)^n - 1}$$
* $P$ = Principal amount (`BigDecimal`)
* $r$ = Monthly interest rate = $\frac{\text{Annual Rate}}{12 \times 100}$
* $n$ = Loan tenure in months

### 4.2 Affordability Assessment (FOIR)
$$\text{FOIR} = \frac{\text{Existing Monthly Obligations} + \text{Rounded EMI}}{\text{Monthly Net Income}} \times 100$$
* Policy threshold: $\text{FOIR} \le 50.00\%$ is required for approval.
* All financial values use `BigDecimal` with `RoundingMode.HALF_UP` to prevent floating-point inaccuracies. Amounts are formatted in Indian numbering (`##,##,###,###.##`).

---

## 5. ACID Transaction Management
All workflow operations modifying multiple tables (`loan_application`, `disbursement`, `application_history`, `ng_core_banking`) are bound inside single database transactions:
```java
Connection conn = DBConnection.getConnection();
try {
    conn.setAutoCommit(false);
    // 1. Insert Disbursement
    disbursementDAO.createDisbursement(disbursement, conn);
    // 2. Update Application Status
    loanApplicationDAO.update(app, conn);
    // 3. Log History Audit
    historyDAO.logHistory(historyRecord, conn);
    conn.commit();
} catch (Exception e) {
    conn.rollback();
    throw e;
} finally {
    conn.setAutoCommit(true);
    conn.close();
}
```

---

## 6. Background Multithreading Architecture
1. **OverdueReviewUtility:**
   * Utilizes Java's `ScheduledExecutorService`.
   * Evaluates applications waiting $> 2$ calendar days against configurable business date.
   * Inserts idempotent `REVIEW_OVERDUE` alerts (`UNIQUE(application_id, alert_type)`).
   * Generates a date-stamped CSV export: `pending_reviews_YYYYMMDD.csv` with `OverdueFlag`.
2. **CoreDataPushUtility:**
   * Daemon thread that continuously polls for loans reaching `DISBURSEMENT`.
   * Pushes payload to `NG_CORE_BANKING` and transitions status to `EXIT`.
