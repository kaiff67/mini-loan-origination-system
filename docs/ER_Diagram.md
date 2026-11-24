# Entity-Relationship (ER) Diagram
## MINI LOAN ORIGINATION SYSTEM (LOS)

The database consists of **7 logical tables** designed to support end-to-end loan workflows, ACID audit logging, idempotent background alert tracking, and Core Banking synchronization.

---

### Mermaid ER Diagram

```mermaid
erDiagram
    CUSTOMER ||--o{ LOAN_APPLICATION : "applies for"
    LOAN_APPLICATION ||--o| DISBURSEMENT : "disburses to"
    LOAN_APPLICATION ||--o{ APPLICATION_HISTORY : "tracks audit"
    LOAN_APPLICATION ||--o{ SYSTEM_ALERTS : "generates alerts"
    LOAN_APPLICATION ||--o| NG_CORE_BANKING : "synchronizes to"

    CUSTOMER {
        bigint customer_id PK
        string name
        date date_of_birth
        string mobile UK "10 digits unique"
        string address
        string kyc_details
        timestamp created_at
    }

    LOAN_APPLICATION {
        bigint application_id PK
        string application_number UK
        bigint customer_id FK
        string product "PERSONAL / EDUCATION"
        string branch_code
        string branch_name
        decimal requested_amount
        int tenure_months
        decimal monthly_income
        decimal existing_monthly_obligations
        decimal annual_interest_rate
        decimal proposed_emi
        decimal foir_percentage
        boolean eligible
        text eligibility_reasons
        string status "DRAFT, UNDER_REVIEW, CREDIT_APPROVAL, DISBURSEMENT, CORE_BANKING_PUSH, EXIT, REJECTED"
        timestamp created_at
        timestamp submitted_at
        timestamp decided_at
        string reviewer_code
        text decision_reason
    }

    DISBURSEMENT {
        bigint disbursement_id PK
        bigint application_id UK "One disbursement per app"
        string reference_number UK "Globally unique ref"
        decimal amount
        timestamp disbursed_at
    }

    APPLICATION_HISTORY {
        bigint history_id PK
        bigint application_id FK
        string action
        string old_status
        string new_status
        string performed_by
        timestamp performed_at
        text remarks
    }

    SYSTEM_ALERTS {
        bigint alert_id PK
        bigint application_id FK
        string alert_type "REVIEW_OVERDUE"
        timestamp created_at
    }

    UTILITY_RUN {
        bigint run_id PK
        string utility_name
        timestamp started_at
        timestamp completed_at
        string result "SUCCESS / FAILED"
        int applications_processed
        int alerts_created
        text error_message
    }

    NG_CORE_BANKING {
        bigint core_id PK
        bigint CustomerID
        bigint ApplicationID UK "Guarantees zero duplicate pushes"
        string Customer_Name
        string Address
        date DOB
        string KYC_Details
        decimal Approved_Amount
        decimal Disbursed_Amount
        timestamp Disbursed_Date
    }
```

---

### Key Database Constraints & Design Highlights:
1. **Disbursement Integrity:**
   * `disbursement.application_id` has a `UNIQUE` constraint, enforcing at the database level that an application can never be disbursed more than once.
   * `disbursement.reference_number` has a `UNIQUE` constraint, ensuring globally unique payment references.
2. **Idempotent Alerts:**
   * `system_alerts` enforces a `UNIQUE (application_id, alert_type)` constraint, guaranteeing that repeated runs of the overdue utility will never generate duplicate alerts.
3. **Core Banking Synchronization:**
   * `ng_core_banking.ApplicationID` has a `UNIQUE` constraint, guaranteeing zero duplicate entries in the Core Banking table.
4. **Validation Constraints:**
   * `CHECK (CHAR_LENGTH(mobile) = 10)` enforces valid mobile numbers.
   * `CHECK (monthly_income > 0)` and `CHECK (existing_monthly_obligations >= 0)` enforce financial integrity.
