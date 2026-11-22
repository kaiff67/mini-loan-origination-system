package com.lending.los.model;

import java.math.BigDecimal;

/**
 * Defines loan products with configurable business parameters.
 */
public enum LoanProduct {
    PERSONAL(
            "Personal Loan",
            new BigDecimal("50000.00"),
            new BigDecimal("500000.00"),
            12,
            60,
            new BigDecimal("20000.00"),
            new BigDecimal("12.00"),
            true
    ),
    EDUCATION(
            "Education Loan",
            new BigDecimal("100000.00"),
            new BigDecimal("1000000.00"),
            12,
            84,
            new BigDecimal("15000.00"),
            new BigDecimal("10.00"),
            false // No co-borrower allowed
    );

    private final String displayName;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private int minTenureMonths;
    private int maxTenureMonths;
    private BigDecimal minMonthlyIncome;
    private BigDecimal annualInterestRate;
    private final boolean coBorrowerAllowed;

    LoanProduct(String displayName, BigDecimal minAmount, BigDecimal maxAmount,
                int minTenureMonths, int maxTenureMonths,
                BigDecimal minMonthlyIncome, BigDecimal annualInterestRate,
                boolean coBorrowerAllowed) {
        this.displayName = displayName;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.minTenureMonths = minTenureMonths;
        this.maxTenureMonths = maxTenureMonths;
        this.minMonthlyIncome = minMonthlyIncome;
        this.annualInterestRate = annualInterestRate;
        this.coBorrowerAllowed = coBorrowerAllowed;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public int getMinTenureMonths() {
        return minTenureMonths;
    }

    public void setMinTenureMonths(int minTenureMonths) {
        this.minTenureMonths = minTenureMonths;
    }

    public int getMaxTenureMonths() {
        return maxTenureMonths;
    }

    public void setMaxTenureMonths(int maxTenureMonths) {
        this.maxTenureMonths = maxTenureMonths;
    }

    public BigDecimal getMinMonthlyIncome() {
        return minMonthlyIncome;
    }

    public void setMinMonthlyIncome(BigDecimal minMonthlyIncome) {
        this.minMonthlyIncome = minMonthlyIncome;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public void setAnnualInterestRate(BigDecimal annualInterestRate) {
        this.annualInterestRate = annualInterestRate;
    }

    public boolean isCoBorrowerAllowed() {
        return coBorrowerAllowed;
    }
}
