package com.lending.los.service;

import com.lending.los.model.Customer;
import com.lending.los.model.LoanApplication;
import com.lending.los.model.LoanProduct;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Core business rule validation and automated underwriting engine.
 */
public class EligibilityEngine {

    public static final int MIN_AGE = 21;
    public static final int MAX_AGE = 60;

    /**
     * Evaluates a loan application against all product, customer, and financial rules.
     * Collects and reports ALL failed rules.
     */
    public static ValidationResult evaluate(Customer customer, LoanApplication app, LocalDate businessDate, boolean hasActiveAppSameProduct) {
        List<String> failedRules = new ArrayList<>();

        // 1. Customer Date of Birth & Age Verification
        if (customer.getDateOfBirth() == null) {
            failedRules.add("Date of birth is required");
        } else if (customer.getDateOfBirth().isAfter(businessDate)) {
            failedRules.add("Future date of birth is strictly rejected (" + customer.getDateOfBirth() + ")");
        } else {
            int age = Period.between(customer.getDateOfBirth(), businessDate).getYears();
            if (age < MIN_AGE || age > MAX_AGE) {
                failedRules.add("Customer age (" + age + " years) outside allowed limits [21 to 60 completed years]");
            }
        }

        // 2. Mobile validation
        if (customer.getMobile() == null || !customer.getMobile().matches("^\\d{10}$")) {
            failedRules.add("Mobile number must contain exactly 10 digits");
        }

        // 3. Duplicate Active Application Check
        if (hasActiveAppSameProduct) {
            failedRules.add("Customer already has an active application for " + app.getProduct().getDisplayName() + ". (Active = DRAFT, UNDER_REVIEW, CREDIT_APPROVAL)");
        }

        // 4. Financial Inputs Validation
        if (app.getMonthlyIncome() == null || app.getMonthlyIncome().compareTo(BigDecimal.ZERO) <= 0) {
            failedRules.add("Monthly net income must be strictly positive");
        }
        if (app.getExistingMonthlyObligations() == null || app.getExistingMonthlyObligations().compareTo(BigDecimal.ZERO) < 0) {
            failedRules.add("Existing monthly obligations must be non-negative");
        }
        if (app.getTenureMonths() <= 0) {
            failedRules.add("Tenure in months must be a positive whole number");
        }

        // 5. Product Specific Rule Checks
        LoanProduct product = app.getProduct();
        if (product != null) {
            // Requested Amount Range
            if (app.getRequestedAmount() == null ||
                    app.getRequestedAmount().compareTo(product.getMinAmount()) < 0 ||
                    app.getRequestedAmount().compareTo(product.getMaxAmount()) > 0) {
                failedRules.add("Requested amount INR " + FinancialCalculator.formatIndianCurrency(app.getRequestedAmount()) +
                        " outside allowed range [" + FinancialCalculator.formatIndianCurrency(product.getMinAmount()) +
                        " to " + FinancialCalculator.formatIndianCurrency(product.getMaxAmount()) + "] for " + product.getDisplayName());
            }

            // Tenure Range
            if (app.getTenureMonths() < product.getMinTenureMonths() ||
                    app.getTenureMonths() > product.getMaxTenureMonths()) {
                failedRules.add("Tenure (" + app.getTenureMonths() + " months) outside allowed range [" +
                        product.getMinTenureMonths() + " to " + product.getMaxTenureMonths() + " months] for " + product.getDisplayName());
            }

            // Minimum Monthly Net Income
            if (app.getMonthlyIncome() != null && app.getMonthlyIncome().compareTo(product.getMinMonthlyIncome()) < 0) {
                failedRules.add("Monthly net income INR " + FinancialCalculator.formatIndianCurrency(app.getMonthlyIncome()) +
                        " is below minimum threshold INR " + FinancialCalculator.formatIndianCurrency(product.getMinMonthlyIncome()) +
                        " for " + product.getDisplayName());
            }
        } else {
            failedRules.add("Loan product must be specified (PERSONAL or EDUCATION)");
        }

        // 6. EMI & FOIR Calculation (if basic financial numbers are valid)
        BigDecimal calculatedEmi = BigDecimal.ZERO;
        BigDecimal calculatedFoir = BigDecimal.ZERO;

        if (app.getRequestedAmount() != null && app.getRequestedAmount().compareTo(BigDecimal.ZERO) > 0 &&
                app.getTenureMonths() > 0 && product != null &&
                app.getMonthlyIncome() != null && app.getMonthlyIncome().compareTo(BigDecimal.ZERO) > 0) {

            calculatedEmi = FinancialCalculator.calculateEMI(app.getRequestedAmount(), product.getAnnualInterestRate(), app.getTenureMonths());
            calculatedFoir = FinancialCalculator.calculateFOIR(app.getExistingMonthlyObligations(), calculatedEmi, app.getMonthlyIncome());

            // FOIR threshold check (max 50.00%)
            if (calculatedFoir.compareTo(FinancialCalculator.MAX_FOIR_THRESHOLD) > 0) {
                failedRules.add("FOIR " + calculatedFoir + "% exceeds maximum allowable limit of 50.00%");
            }
        }

        boolean eligible = failedRules.isEmpty();
        return new ValidationResult(eligible, calculatedEmi, calculatedFoir, failedRules);
    }
}
