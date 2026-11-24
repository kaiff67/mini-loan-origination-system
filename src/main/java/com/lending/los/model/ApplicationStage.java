package com.lending.los.model;

/**
 * Workflow stages of the loan lifecycle.
 * Normal Flow: DRAFT -> UNDER_REVIEW -> CREDIT_APPROVAL -> DISBURSEMENT -> CORE_BANKING_PUSH -> EXIT
 * Rejection: DRAFT / UNDER_REVIEW / CREDIT_APPROVAL -> REJECTED
 */
public enum ApplicationStage {
    DRAFT("Application is being prepared by customer or officer"),
    UNDER_REVIEW("Application passed automated checks, awaiting reviewer decision"),
    CREDIT_APPROVAL("Approved by reviewer, awaiting final credit signoff and doc generation"),
    DISBURSEMENT("Simulated loan disbursement completed atomically"),
    CORE_BANKING_PUSH("System stage where background utility pushes record to NG_CORE_BANKING"),
    EXIT("Final terminal state post successful Core Banking push"),
    REJECTED("Terminal rejection stage; view rejection reasons only, no further changes");

    private final String description;

    ApplicationStage(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isTerminal() {
        return this == EXIT || this == REJECTED;
    }

    public boolean isActive() {
        return this == DRAFT || this == UNDER_REVIEW || this == CREDIT_APPROVAL;
    }
}
