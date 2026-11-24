package com.lending.los.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * High-precision financial calculator for EMI and FOIR computations.
 * Implements standard reducing-balance compound interest and debt-to-income affordability calculations:
 *
 * EMI = P * r * (1+r)^n / ((1+r)^n - 1)
 * FOIR = (existing_obligations + rounded_EMI) / monthly_income * 100
 *
 * All amounts formatted in Indian Numbering Format (##,##,###,###.##).
 */
public class FinancialCalculator {

    private static final MathContext MC = new MathContext(16, RoundingMode.HALF_UP);
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal TWELVE = new BigDecimal("12.00");
    public static final BigDecimal MAX_FOIR_THRESHOLD = new BigDecimal("50.00");

    /**
     * Calculates Monthly EMI with high precision using BigDecimal and double power function.
     *
     * @param principal requested amount (P)
     * @param annualInterestRate annual interest rate as percentage (e.g. 12.00)
     * @param tenureMonths tenure in months (n)
     * @return rounded monthly EMI to 2 decimal places (HALF_UP)
     */
    public static BigDecimal calculateEMI(BigDecimal principal, BigDecimal annualInterestRate, int tenureMonths) {
        if (principal == null || annualInterestRate == null || tenureMonths <= 0) {
            throw new IllegalArgumentException("Principal, interest rate, and tenure must be valid positive values.");
        }

        // r = annual rate / 12 / 100
        double r = annualInterestRate.doubleValue() / 12.0 / 100.0;
        double p = principal.doubleValue();
        int n = tenureMonths;

        if (r == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }

        double numerator = p * r * Math.pow(1.0 + r, n);
        double denominator = Math.pow(1.0 + r, n) - 1.0;
        double emi = numerator / denominator;

        return BigDecimal.valueOf(emi).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates Fixed Obligation to Income Ratio (FOIR).
     * FOIR = (existing_obligations + rounded_EMI) / monthly_income * 100
     *
     * @param existingObligations existing monthly obligations
     * @param roundedEmi proposed monthly EMI
     * @param monthlyIncome monthly net income
     * @return FOIR percentage rounded to 2 decimal places
     */
    public static BigDecimal calculateFOIR(BigDecimal existingObligations, BigDecimal roundedEmi, BigDecimal monthlyIncome) {
        if (monthlyIncome == null || monthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monthly net income must be strictly positive.");
        }
        if (existingObligations == null) {
            existingObligations = BigDecimal.ZERO;
        }
        if (roundedEmi == null) {
            roundedEmi = BigDecimal.ZERO;
        }

        BigDecimal totalObligations = existingObligations.add(roundedEmi);
        return totalObligations
                .multiply(HUNDRED)
                .divide(monthlyIncome, 2, RoundingMode.HALF_UP);
    }

    /**
     * Formats amounts in Indian Numbering System: ##,##,###,###.##
     * Example: 100000.00 -> 1,00,000.00
     */
    public static String formatIndianCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        // Indian numbering system format: ##,##,###.##
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("en", "IN"));
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');

        DecimalFormat df = new DecimalFormat("##,##,##,##,##0.00", symbols);
        return df.format(amount.setScale(2, RoundingMode.HALF_UP));
    }
}
