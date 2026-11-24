package com.lending.los.ui;

import com.lending.los.dao.*;
import com.lending.los.model.*;
import com.lending.los.service.*;
import com.lending.los.utility.CoreDataPushUtility;
import com.lending.los.utility.OverdueReviewUtility;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

/**
 * Menu-driven interactive Console Application for Newgen Loan Origination System.
 */
public class ConsoleMenu {

    private final Scanner scanner = new Scanner(System.in);
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final LoanApplicationDAO loanApplicationDAO = new LoanApplicationDAO();
    private final ReportDAO reportDAO = new ReportDAO();
    private final SystemAlertDAO alertDAO = new SystemAlertDAO();
    private final ApplicationWorkflowService workflowService = new ApplicationWorkflowService();
    private final OverdueReviewUtility overdueUtility;
    private final CoreDataPushUtility corePushUtility;

    public ConsoleMenu(OverdueReviewUtility overdueUtility, CoreDataPushUtility corePushUtility) {
        this.overdueUtility = overdueUtility;
        this.corePushUtility = corePushUtility;
    }

    public void start() {
        while (true) {
            printHeader();
            System.out.println(" 1. Register New Customer");
            System.out.println(" 2. Search / View Customers");
            System.out.println(" 3. Create DRAFT Loan Application");
            System.out.println(" 4. Submit Loan Application (Run Auto-Eligibility)");
            System.out.println(" 5. Review Application (Reviewer Approve / Reject)");
            System.out.println(" 6. Credit Approval & Generate Application Form");
            System.out.println(" 7. Disburse Approved Loan");
            System.out.println(" 8. Six SQL Analytics Reports");
            System.out.println(" 9. Background Utilities & Alerts");
            System.out.println("10. Test Scenarios (Date Simulation & Controlled Rollback)");
            System.out.println(" 0. Exit Application");
            System.out.println("================================================================================");
            System.out.print("Enter your choice [0-10]: ");
            System.out.flush();

            if (!scanner.hasNextLine()) break;
            String choice = scanner.nextLine().trim();
            if (choice.isEmpty()) continue;
            try {
                switch (choice) {
                    case "1" -> registerCustomer();
                    case "2" -> searchCustomers();
                    case "3" -> createDraftApplication();
                    case "4" -> submitApplication();
                    case "5" -> reviewApplication();
                    case "6" -> creditApproval();
                    case "7" -> disburseLoan();
                    case "8" -> reportsMenu();
                    case "9" -> utilitiesMenu();
                    case "10" -> testScenariosMenu();
                    case "0" -> {
                        System.out.println("\nExiting Loan Origination System. Goodbye!");
                        return;
                    }
                    default -> System.out.println("[WARN] Invalid option. Please select 0-10.");
                }
            } catch (Exception e) {
                System.out.println("\n[ERROR] " + e.getMessage());
            }
            System.out.println("\nPress [ENTER] to continue...");
            scanner.nextLine();
        }
    }

    private void printHeader() {
        System.out.println("\n================================================================================");
        System.out.println("                 MINI LOAN ORIGINATION SYSTEM (LOS)");
        System.out.println("              Configured Business Date: " + BusinessDateService.getFormattedBusinessDate());
        System.out.println("================================================================================");
    }

    private void registerCustomer() throws Exception {
        System.out.println("\n--- 1. REGISTER NEW CUSTOMER ---");
        System.out.print("Enter Full Name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Enter Date of Birth (YYYY-MM-DD): ");
        LocalDate dob = LocalDate.parse(scanner.nextLine().trim());

        System.out.print("Enter 10-digit Mobile Number: ");
        String mobile = scanner.nextLine().trim();

        System.out.print("Enter Address: ");
        String address = scanner.nextLine().trim();

        System.out.print("Enter KYC ID / Details (e.g. PAN / Aadhaar): ");
        String kyc = scanner.nextLine().trim();

        Customer customer = new Customer(name, dob, mobile, address, kyc);
        customer.setCreatedAt(BusinessDateService.getBusinessDateTime());

        Long id = customerDAO.createCustomer(customer);
        System.out.println("\n[SUCCESS] Customer registered successfully with ID: " + id);
    }

    private void searchCustomers() throws Exception {
        System.out.println("\n--- 2. SEARCH / VIEW CUSTOMERS ---");
        System.out.print("Enter search keyword (Customer ID, Mobile, or Name) or press Enter to view all: ");
        System.out.flush();
        String query = scanner.nextLine().trim();

        List<Customer> list = query.isEmpty() ? customerDAO.findAll() : customerDAO.searchCustomers(query);
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.printf("%-5s | %-20s | %-12s | %-12s | %-25s%n", "ID", "Name", "DOB", "Mobile", "KYC Details");
        System.out.println("--------------------------------------------------------------------------------");
        for (Customer c : list) {
            System.out.printf("%-5d | %-20s | %-12s | %-12s | %-25s%n",
                    c.getCustomerId(), c.getName(), c.getDateOfBirth(), c.getMobile(), c.getKycDetails());
        }
        System.out.println("--------------------------------------------------------------------------------");
    }

    private void createDraftApplication() throws Exception {
        System.out.println("\n--- 3. CREATE DRAFT LOAN APPLICATION ---");
        System.out.print("Enter Customer ID: ");
        Long customerId = Long.parseLong(scanner.nextLine().trim());

        System.out.print("Select Product (1 for PERSONAL, 2 for EDUCATION): ");
        String prodChoice = scanner.nextLine().trim();
        LoanProduct product = prodChoice.equals("2") ? LoanProduct.EDUCATION : LoanProduct.PERSONAL;

        System.out.print("Enter Branch Code (e.g. BR-DEL-01): ");
        String branchCode = scanner.nextLine().trim();
        System.out.print("Enter Branch Name: ");
        String branchName = scanner.nextLine().trim();

        System.out.print("Enter Requested Loan Amount (INR): ");
        BigDecimal amount = new BigDecimal(scanner.nextLine().trim());

        System.out.print("Enter Tenure in Months: ");
        int tenure = Integer.parseInt(scanner.nextLine().trim());

        System.out.print("Enter Monthly Net Income (INR): ");
        BigDecimal income = new BigDecimal(scanner.nextLine().trim());

        System.out.print("Enter Existing Monthly Obligations (INR): ");
        BigDecimal obligations = new BigDecimal(scanner.nextLine().trim());

        LoanApplication app = workflowService.createDraft(customerId, product, branchCode, branchName,
                amount, tenure, income, obligations);

        System.out.println("\n[SUCCESS] Draft Application Created Successfully!");
        System.out.println("Application Number : " + app.getApplicationNumber());
        System.out.println("Proposed EMI       : INR " + FinancialCalculator.formatIndianCurrency(app.getProposedEmi()));
        System.out.println("Estimated FOIR     : " + app.getFoirPercentage() + "%");
        System.out.println("Current Status     : " + app.getStatus());
    }

    private void submitApplication() throws Exception {
        System.out.println("\n--- 4. SUBMIT LOAN APPLICATION (AUTO-ELIGIBILITY CHECK) ---");
        System.out.print("Enter Application ID to Submit: ");
        Long appId = Long.parseLong(scanner.nextLine().trim());

        ValidationResult result = workflowService.submitApplication(appId, "LOAN_OFFICER_01");

        System.out.println("\n========================================================");
        System.out.println("             AUTOMATED UNDERWRITING RESULT");
        System.out.println("========================================================");
        System.out.println("Calculated EMI : INR " + FinancialCalculator.formatIndianCurrency(result.getCalculatedEmi()));
        System.out.println("Calculated FOIR: " + result.getCalculatedFoir() + "% (Threshold <= 50.00%)");
        System.out.println("Eligibility    : " + (result.isEligible() ? "ELIGIBLE -> Moved to UNDER_REVIEW" : "INELIGIBLE -> Moved to REJECTED"));

        if (!result.isEligible()) {
            System.out.println("\nFailed Rules:");
            for (String reason : result.getFailureReasons()) {
                System.out.println("  ❌ " + reason);
            }
        }
        System.out.println("========================================================");
    }

    private void reviewApplication() throws Exception {
        System.out.println("\n--- 5. REVIEW APPLICATION (UNDER_REVIEW) ---");
        System.out.print("Enter Application ID to Review: ");
        Long appId = Long.parseLong(scanner.nextLine().trim());

        System.out.print("Decision (1 = APPROVE, 2 = REJECT): ");
        String decisionChoice = scanner.nextLine().trim();
        boolean approve = decisionChoice.equals("1");

        System.out.print("Enter Decision Remarks / Reason" + (!approve ? " (MANDATORY for rejection)" : " (Optional)") + ": ");
        String reason = scanner.nextLine().trim();

        System.out.print("Enter Reviewer Code (e.g. REV_PRIYA): ");
        String reviewerCode = scanner.nextLine().trim();
        if (reviewerCode.isEmpty()) reviewerCode = "REV_MANAGER";

        workflowService.reviewDecision(appId, approve, reason, reviewerCode);
        System.out.println("\n[SUCCESS] Review decision recorded successfully! Status moved to: " +
                (approve ? "CREDIT_APPROVAL" : "REJECTED"));
    }

    private void creditApproval() throws Exception {
        System.out.println("\n--- 6. CREDIT APPROVAL & APPLICATION FORM GENERATION ---");
        System.out.print("Enter Application ID: ");
        Long appId = Long.parseLong(scanner.nextLine().trim());

        File doc = workflowService.performCreditApproval(appId, "CREDIT_HEAD_01");
        System.out.println("\n[SUCCESS] Credit approval confirmed!");
        System.out.println("Official Application Form generated at: " + doc.getAbsolutePath());
    }

    private void disburseLoan() throws Exception {
        System.out.println("\n--- 7. DISBURSE APPROVED LOAN ---");
        System.out.print("Enter Application ID to Disburse: ");
        Long appId = Long.parseLong(scanner.nextLine().trim());

        System.out.print("Enter Globally Unique Disbursement Reference (e.g. DISB-20261026-001): ");
        String ref = scanner.nextLine().trim();

        Disbursement d = workflowService.disburseLoan(appId, ref, "DISB_OFFICER_01");
        System.out.println("\n[SUCCESS] Loan Disbursed Atomically!");
        System.out.println("Disbursement ID  : " + d.getDisbursementId());
        System.out.println("Reference Number : " + d.getReferenceNumber());
        System.out.println("Disbursed Amount : INR " + FinancialCalculator.formatIndianCurrency(d.getAmount()));
        System.out.println("Disbursed At     : " + d.getDisbursedAt());
        System.out.println("Application Stage: DISBURSEMENT (will be picked by CoreDataPush to move to EXIT)");
    }

    private void reportsMenu() throws Exception {
        System.out.println("\n================================================================================");
        System.out.println("                       SIX MANDATORY SQL REPORTS");
        System.out.println("================================================================================");
        System.out.println("1. Application Register (Filter by Product & Status)");
        System.out.println("2. Product Pipeline (By Product/Status and By Branch/Product)");
        System.out.println("3. Daily / Monthly / Quarterly Disbursement Summary");
        System.out.println("4. Overdue Reviews (> 2 Calendar Days)");
        System.out.println("5. Customers with Application but No Disbursement");
        System.out.println("6. Repeat Applicants (GROUP BY & HAVING)");
        System.out.println("0. Back to Main Menu");
        System.out.print("Select Report [0-6]: ");
        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1" -> {
                System.out.print("Enter Product filter (PERSONAL, EDUCATION, or ALL): ");
                String p = scanner.nextLine().trim().toUpperCase();
                System.out.print("Enter Status filter (DRAFT, UNDER_REVIEW, CREDIT_APPROVAL, DISBURSEMENT, EXIT, REJECTED, or ALL): ");
                String s = scanner.nextLine().trim().toUpperCase();
                reportDAO.printApplicationRegister(p.isEmpty() ? "ALL" : p, s.isEmpty() ? "ALL" : s);
            }
            case "2" -> reportDAO.printProductPipeline();
            case "3" -> {
                System.out.print("Enter Start Date (YYYY-MM-DD): ");
                String from = scanner.nextLine().trim();
                System.out.print("Enter End Date (YYYY-MM-DD): ");
                String to = scanner.nextLine().trim();
                reportDAO.printDisbursementSummary(
                        LocalDate.parse(from).atStartOfDay(),
                        LocalDate.parse(to).atTime(23, 59, 59));
            }
            case "4" -> reportDAO.printOverdueReviews(BusinessDateService.getBusinessDate());
            case "5" -> reportDAO.printCustomersWithoutDisbursement();
            case "6" -> reportDAO.printRepeatApplicants();
        }
    }

    private void utilitiesMenu() throws Exception {
        System.out.println("\n================================================================================");
        System.out.println("                         BACKGROUND UTILITIES");
        System.out.println("================================================================================");
        System.out.println("1. Run Overdue Review Utility Now (Scan, generate alerts, export CSV)");
        System.out.println("2. Run CoreDataPush Utility Now (Push DISBURSEMENT to Core Table -> EXIT)");
        System.out.println("3. View Active System Alerts");
        System.out.print("Select Option [1-3]: ");
        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1" -> {
                OverdueReviewUtility.UtilityRunResult res = overdueUtility.executeUtility();
                System.out.println("\n[UTILITY RESULT] Applications Processed: " + res.applicationsProcessed);
                System.out.println("[UTILITY RESULT] New Alerts Created     : " + res.alertsCreated);
                if (res.csvFile != null) {
                    System.out.println("[UTILITY RESULT] CSV Exported To       : " + res.csvFile.getAbsolutePath());
                }
            }
            case "2" -> {
                int pushed = corePushUtility.pushDisbursedCasesToCoreBanking();
                System.out.println("\n[UTILITY RESULT] Cases pushed to Core Banking and moved to EXIT: " + pushed);
            }
            case "3" -> {
                List<SystemAlert> alerts = alertDAO.findAll();
                System.out.println("\n------------------------------------------------------------------");
                System.out.printf("%-10s | %-16s | %-20s | %-20s%n", "Alert ID", "Application ID", "Alert Type", "Created At");
                System.out.println("------------------------------------------------------------------");
                for (SystemAlert a : alerts) {
                    System.out.printf("%-10d | %-16d | %-20s | %-20s%n",
                            a.getAlertId(), a.getApplicationId(), a.getAlertType(), a.getCreatedAt());
                }
                System.out.println("------------------------------------------------------------------");
            }
        }
    }

    private void testScenariosMenu() {
        System.out.println("\n================================================================================");
        System.out.println("             TEST SCENARIOS & DATE SIMULATION (SLIDE 13)");
        System.out.println("================================================================================");
        System.out.println("1. Advance Business Date by Days (e.g. simulate 2 or 3 days aging)");
        System.out.println("2. Set Explicit Business Date (YYYY-MM-DD)");
        System.out.println("3. Run Controlled Transaction Rollback Demo (Scenario 09)");
        System.out.print("Select Option [1-3]: ");
        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1" -> {
                System.out.print("Enter number of days to advance (e.g. 3): ");
                long days = Long.parseLong(scanner.nextLine().trim());
                BusinessDateService.advanceDays(days);
                System.out.println("[SUCCESS] New Business Date: " + BusinessDateService.getFormattedBusinessDate());
            }
            case "2" -> {
                System.out.print("Enter new Business Date (YYYY-MM-DD): ");
                LocalDate newDate = LocalDate.parse(scanner.nextLine().trim());
                BusinessDateService.setBusinessDate(newDate);
                System.out.println("[SUCCESS] Business Date set to: " + BusinessDateService.getFormattedBusinessDate());
            }
            case "3" -> {
                System.out.print("Enter Application ID to demonstrate controlled transaction failure: ");
                Long id = Long.parseLong(scanner.nextLine().trim());
                boolean passed = workflowService.demonstrateControlledRollback(id);
                if (passed) {
                    System.out.println("\n[DEMO VERIFIED] Scenario 09 PASSED! Controlled failure triggered rollback successfully.");
                }
            }
        }
    }
}
