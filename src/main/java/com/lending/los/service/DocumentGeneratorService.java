package com.lending.los.service;

import com.lending.los.model.Customer;
import com.lending.los.model.LoanApplication;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;

/**
 * Generates the formal Application Form document when an application passes Credit Approval.
 * As required by business specification:
 * "Once Credit Approval is submitted, generate the Application Form document
 *  (applicant, loan and approval details) as a document-generation pointer/trigger."
 */
public class DocumentGeneratorService {

    private static final String DOCS_DIR = "generated_documents";

    public static File generateApplicationForm(Customer customer, LoanApplication app) throws IOException {
        File dir = new File(DOCS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = "Application_Form_" + app.getApplicationNumber() + ".txt";
        File file = new File(dir, fileName);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMMM-yyyy HH:mm:ss");

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("================================================================================");
            writer.println("                   NEWGEN LOAN ORIGINATION SYSTEM (LOS)");
            writer.println("                       OFFICIAL APPLICATION FORM");
            writer.println("================================================================================");
            writer.println("Generated Date : " + BusinessDateService.getBusinessDateTime().format(dtf));
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
            writer.println("Decision Date  : " + (app.getDecidedAt() != null ? app.getDecidedAt().format(dtf) : "N/A"));
            writer.println("Review Remarks : " + (app.getDecisionReason() != null ? app.getDecisionReason() : "N/A"));
            writer.println("Eligibility    : " + (app.isEligible() ? "PASSED" : "FAILED"));
            writer.println("================================================================================");
            writer.println("Document Authentication: Digitally generated and sealed by LOS Application Engine.");
            writer.println("================================================================================");
        }

        return file;
    }
}
