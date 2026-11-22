package com.lending.los.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the complete result of automated eligibility assessment.
 */
public class ValidationResult {
    private final boolean eligible;
    private final BigDecimal calculatedEmi;
    private final BigDecimal calculatedFoir;
    private final List<String> failureReasons;

    public ValidationResult(boolean eligible, BigDecimal calculatedEmi, BigDecimal calculatedFoir, List<String> failureReasons) {
        this.eligible = eligible;
        this.calculatedEmi = calculatedEmi;
        this.calculatedFoir = calculatedFoir;
        this.failureReasons = failureReasons != null ? new ArrayList<>(failureReasons) : new ArrayList<>();
    }

    public boolean isEligible() {
        return eligible;
    }

    public BigDecimal getCalculatedEmi() {
        return calculatedEmi;
    }

    public BigDecimal getCalculatedFoir() {
        return calculatedFoir;
    }

    public List<String> getFailureReasons() {
        return Collections.unmodifiableList(failureReasons);
    }

    public String getReasonsAsFormattedString() {
        if (failureReasons.isEmpty()) {
            return "All eligibility checks passed successfully.";
        }
        return String.join("; ", failureReasons);
    }
}
