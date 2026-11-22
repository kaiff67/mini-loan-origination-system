package com.lending.los.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Disbursement entity representing loan disbursement details.
 */
public class Disbursement {
    private Long disbursementId;
    private Long applicationId;
    private String referenceNumber;
    private BigDecimal amount;
    private LocalDateTime disbursedAt;

    public Disbursement() {}

    public Disbursement(Long applicationId, String referenceNumber, BigDecimal amount, LocalDateTime disbursedAt) {
        this.applicationId = applicationId;
        this.referenceNumber = referenceNumber;
        this.amount = amount;
        this.disbursedAt = disbursedAt;
    }

    public Long getDisbursementId() {
        return disbursementId;
    }

    public void setDisbursementId(Long disbursementId) {
        this.disbursementId = disbursementId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getDisbursedAt() {
        return disbursedAt;
    }

    public void setDisbursedAt(LocalDateTime disbursedAt) {
        this.disbursedAt = disbursedAt;
    }
}
