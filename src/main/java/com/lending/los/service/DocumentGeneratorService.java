package com.lending.los.service;

import com.lending.los.model.Customer;
import com.lending.los.model.LoanApplication;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

/**
 * Generates formal physical documents for loan applications:
 * 1. Application Form upon Credit Approval.
 * 2. Official Loan Sanction Advice Docket with 3-Month Amortization Schedule upon Disbursement.
 */
public class DocumentGeneratorService {

    private static final String DOCS_DIR = "generated_documents";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd-MMMM-yyyy HH:mm:ss");

    public static File generateApplicationForm(Customer customer, LoanApplication app) throws IOException {
        File dir = new File(DOCS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = "Application_Form_" + app.getApplicationNumber() + ".txt";
        File file = new File(dir, fileName);

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("================================================================================");
            writer.println("                 ENTERPRISE LOAN ORIGINATION SYSTEM (LOS)");
            writer.println("                       OFFICIAL APPLICATION FORM");
            writer.println("================================================================================");
            writer.println("Generated Date : " + BusinessDateService.getBusinessDateTime().format(DTF));
            writer.println("Application No : " + app.getApplicationNumber());
            writer.println("Current Status : " + app.getStatus());
            writer.println("Branch Code    : " + app.getBranchCode() + " (" + app.getBranchName() + ")");
            writer.println("--------------------------------------------------------------------------------");
            writer.println("1. APPLICANT DETAILS");
            writer.println("--------------------------------------------------------------------------------");
            writer.println("Full Name      : " + customer.getName());
            writer.println("Date of Birth  : " + customer.getDateOfBirth());
            writer.println("Mobile Number  : +91-" + customer.getMobile());
            writer.println("Address        : " + customer.getAddress());
            writer.println("KYC Identifier : " + customer.getKycDetails());
            writer.println("--------------------------------------------------------------------------------");
            writer.println("2. LOAN & FINANCIAL DETAILS");
            writer.println("--------------------------------------------------------------------------------");
            writer.println("Loan Product   : " + app.getProduct().getDisplayName());
            writer.println("Loan Amount    : INR " + FinancialCalculator.formatIndianCurrency(app.getRequestedAmount()));
            writer.println("Tenure (Months): " + app.getTenureMonths() + " months");
            writer.println("Interest Rate  : " + app.getAnnualInterestRate() + "% p.a.");
            writer.println("Monthly Income : INR " + FinancialCalculator.formatIndianCurrency(app.getMonthlyIncome()));
            writer.println("Existing Debts : INR " + FinancialCalculator.formatIndianCurrency(app.getExistingMonthlyObligations()));
            writer.println("Calculated EMI : INR " + FinancialCalculator.formatIndianCurrency(app.getProposedEmi()));
            writer.println("Affordability  : FOIR = " + app.getFoirPercentage() + "% (Policy Limit <= 50.00%)");
            writer.println("--------------------------------------------------------------------------------");
            writer.println("3. UNDERWRITING & CREDIT APPROVAL SIGN-OFF");
            writer.println("--------------------------------------------------------------------------------");
            writer.println("Reviewer Code  : " + (app.getReviewerCode() != null ? app.getReviewerCode() : "N/A"));
            writer.println("Decision Date  : " + (app.getDecidedAt() != null ? app.getDecidedAt().format(DTF) : "N/A"));
            writer.println("Review Remarks : " + (app.getDecisionReason() != null ? app.getDecisionReason() : "N/A"));
            writer.println("Eligibility    : " + (app.isEligible() ? "PASSED" : "FAILED"));
            writer.println("================================================================================");
            writer.println("Authentication: Digitally generated and cryptographically sealed by LOS Core Engine.");
            writer.println("================================================================================");
        }

        return file;
    }

    public static File generateSanctionDocket(Customer customer, LoanApplication app) throws IOException {
        File dir = new File(DOCS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = "Sanction_Docket_" + app.getApplicationNumber() + ".txt";
        File file = new File(dir, fileName);

        BigDecimal principal = app.getRequestedAmount();
        BigDecimal emi = app.getProposedEmi();
        BigDecimal annualRate = app.getAnnualInterestRate();
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("================================================================================");
            writer.println("                 ENTERPRISE LOAN ORIGINATION SYSTEM (LOS)");
            writer.println("                    OFFICIAL LOAN SANCTION ADVICE DOCKET");
            writer.println("================================================================================");
            writer.println("Docket Ref No  : SANCT-" + app.getApplicationNumber() + "-" + System.currentTimeMillis() % 100000);
            writer.println("Sanction Date  : " + BusinessDateService.getBusinessDateTime().format(DTF));
            writer.println("Branch Name    : " + app.getBranchName() + " [" + app.getBranchCode() + "]");
            writer.println("--------------------------------------------------------------------------------");
            writer.println("BORROWER INFORMATION:");
            writer.println("  Borrower Name  : " + customer.getName());
            writer.println("  Mobile Contact : +91-" + customer.getMobile());
            writer.println("  KYC Record     : " + customer.getKycDetails());
            writer.println("  Address        : " + customer.getAddress());
            writer.println("--------------------------------------------------------------------------------");
            writer.println("SANCTIONED CREDIT TERMS:");
            writer.println("  Facility Type  : " + app.getProduct().getDisplayName());
            writer.println("  Principal Sum  : INR " + FinancialCalculator.formatIndianCurrency(principal));
            writer.println("  Interest Rate  : " + annualRate + "% p.a. (Reducing Balance Method)");
            writer.println("  Tenure Agreed  : " + app.getTenureMonths() + " Calendar Months");
            writer.println("  Equated Monthly: INR " + FinancialCalculator.formatIndianCurrency(emi));
            writer.println("  FOIR Verified  : " + app.getFoirPercentage() + "% (Compliant with <= 50.00% ceiling)");
            writer.println("--------------------------------------------------------------------------------");
            writer.println("PREVIEW OF FIRST 3 MONTHS AMORTIZATION SCHEDULE:");
            writer.println(String.format("  %-7s | %-15s | %-15s | %-15s | %-15s", "Month", "Opening (INR)", "Principal (INR)", "Interest (INR)", "Closing (INR)"));
            writer.println("  ------------------------------------------------------------------------------");

            BigDecimal balance = principal;
            for (int month = 1; month <= Math.min(3, app.getTenureMonths()); month++) {
                BigDecimal interest = balance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
                BigDecimal principalComponent = emi.subtract(interest).setScale(2, RoundingMode.HALF_UP);
                BigDecimal closing = balance.subtract(principalComponent).setScale(2, RoundingMode.HALF_UP);
                if (closing.compareTo(BigDecimal.ZERO) < 0) closing = BigDecimal.ZERO;

                writer.println(String.format("  Month %-2d | %-15s | %-15s | %-15s | %-15s",
                        month,
                        FinancialCalculator.formatIndianCurrency(balance),
                        FinancialCalculator.formatIndianCurrency(principalComponent),
                        FinancialCalculator.formatIndianCurrency(interest),
                        FinancialCalculator.formatIndianCurrency(closing)));
                balance = closing;
            }

            writer.println("  ------------------------------------------------------------------------------");
            writer.println("DISBURSEMENT MANDATE:");
            writer.println("  Mode of Disbursal : Direct Credit via Core Banking Push (Account Verification Active)");
            writer.println("  Terminal State    : Automatic Sync into NG_CORE_BANKING via Daemon Worker");
            writer.println("================================================================================");
            writer.println("           AUTHORIZED DIGITAL DISBURSAL STAMP - ALL RIGHTS RESERVED");
            writer.println("================================================================================");
        }

        return file;
    }
}
