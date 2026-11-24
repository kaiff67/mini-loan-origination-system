# MINI LOAN ORIGINATION SYSTEM (LOS)

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Architecture](https://img.shields.io/badge/Architecture-Layered_DAO_Service-blue?style=for-the-badge)

An enterprise-grade, end-to-end Mini Loan Origination System (LOS) built with **Core Java 17**, pure **JDBC**, and **MySQL 8.0**. Implements automated credit underwriting, ACID transaction boundaries, scheduled background daemons, and dual interfaces.

---

## 📋 Overview & Key Features

* **Multi-Stage Lending Workflow:**
  `DRAFT ➔ UNDER_REVIEW ➔ CREDIT_APPROVAL ➔ DISBURSEMENT ➔ CORE_BANKING_PUSH ➔ EXIT`
* **Automated Credit Underwriting (Brain):**
  * Exact EMI calculation via banking amortization formula.
  * FOIR (Fixed Obligation to Income Ratio) calculation with strict 50% policy threshold.
  * Age validation (21 to 60 completed years) and active loan constraints.
  * Currency formatted in Indian numbering (`##,##,###,###.##`).
* **ACID Transaction Management:**
  * Strict multi-table transaction boundaries (`setAutoCommit(false)`, `commit()`, `rollback()`).
  * Full audit trail logging in `application_history`.
* **Scheduled Background Daemons:**
  * **Overdue Review Monitor:** Uses `ScheduledExecutorService` to identify pending reviews older than 2 calendar days, generates alerts, and exports reports to CSV (`pending_reviews_YYYYMMDD.csv`).
  * **Continuous Core Banking Push:** Daemon worker pushing disbursed loans to `NG_CORE_BANKING` and transitioning them to `EXIT`.
* **Six Parameterized SQL Reports:**
  * Application register, product pipeline, disbursement analysis, overdue reviews, customers without disbursement, and repeat applicants.
* **Dual User Experience:**
  * Modern **Java Swing Desktop Dashboard** alongside an interactive **Terminal Console Menu**.
* **Automated Test Suite:**
  * 1-Click test runner (`AutoTestRunner`) programmatically validating 12 business rules and edge cases in ~1.2s.

---

## 🛠 Project Structure

```text
mini-loan-origination/
├── src/main/java/com/lending/los/
│   ├── Main.java                        # System bootstrap & thread pool initialization
│   ├── AutoTestRunner.java              # 1-Click Automated Test & Verification Suite
│   ├── model/                           # Domain Entities & Business Enums
│   ├── dao/                             # Pure JDBC Data Access Objects & SQL Queries
│   ├── service/                         # Business Domain Logic, Validation, Math & Transactions
│   ├── utility/                         # Scheduled & Background Executors
│   └── ui/                              # Swing Desktop GUI & Console CLI
├── src/main/resources/
│   ├── db.properties                    # Database connection properties
│   ├── schema.sql                       # DDL schema definition & constraints (7 tables)
│   ├── seed_data.sql                    # Initial seed data for verification scenarios
│   └── reports.sql                      # Six analytical parameterized queries
├── docs/
│   ├── ER_Diagram.md                    # Database ER diagram & constraint definitions
│   ├── Design_Document.md               # Technical architecture & design documentation
│   └── Test_Cases_Verification.md       # Matrix of all 12 test verification scenarios
└── pom.xml
```

---

## 🚀 Getting Started

### 1. Database Setup (MySQL)
1. Open **MySQL Workbench** and connect to your local MySQL server.
2. Open and execute `src/main/resources/schema.sql` (Creates `loan_origination_db` database and 7 tables).
3. Open and execute `src/main/resources/seed_data.sql` (Populates sample test records).

### 2. Configure Database Credentials
Edit `src/main/resources/db.properties` with your MySQL credentials:
```properties
db.url=jdbc:mysql://localhost:3306/loan_origination_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=YOUR_PASSWORD_HERE
```

### 3. Run the Application
1. Open the project in **IntelliJ IDEA** (`File` ➔ `Open`).
2. Run `Main.java` (click the green **▶** Run button).
3. The interactive console menu will appear in your terminal.

---

## 🧪 12 Verification Scenarios Demonstrated

Refer to [`docs/Test_Cases_Verification.md`](docs/Test_Cases_Verification.md) for full reproduction steps:
1. Product minimum/maximum boundaries
2. Ages 20, 21, 60, and 61 (acceptance vs rejection)
3. Duplicate mobile with active application
4. Worked EMI formula match (₹1,00,000 @ 12% over 12 months = ₹8,884.88)
5. FOIR affordability threshold (46.28% PASS vs 52.95% FAIL)
6. Invalid state transitions rejected
7. Generated Application Form document
8. Duplicate and repeated disbursement attempts blocked
9. Controlled transaction failure with full rollback
10. Review ages of exactly 2 and 3 days (via configurable business date)
11. Repeat utility execution without duplicate alerts
12. Data persistence across application restarts
