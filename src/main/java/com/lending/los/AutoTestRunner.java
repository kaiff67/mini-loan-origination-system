package com.lending.los;

import com.lending.los.dao.*;
import com.lending.los.model.*;
import com.lending.los.service.*;
import com.lending.los.utility.OverdueReviewUtility;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Automated System Verification Suite.
 * Programmatically validates core business rules, boundary conditions, and transaction workflows.
 */
public class AutoTestRunner {

    public static class TestResult {
        public final String testId;
        public final String description;
        public final String expected;
        public final String actual;
        public final boolean passed;
        public final String details;

        public TestResult(String testId, String description, String expected, String actual, boolean passed, String details) {
            this.testId = testId;
            this.description = description;
            this.expected = expected;
            this.actual = actual;
            this.passed = passed;
            this.details = details;
        }
    }

    public static class TestSuiteSummary {
        public final List<TestResult> results = new ArrayList<>();
        public int passedCount = 0;
        public int totalCount = 0;
        public long durationMs = 0;

        public String getFormattedSummaryTable() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n=======================================================================================================\n");
            sb.append("                          AUTOMATED TEST & VERIFICATION SUITE\n");
            sb.append("=======================================================================================================\n");
            sb.append(String.format("%-6s | %-38s | %-16s | %-16s | %-8s\n", "TC #", "Verification Scenario", "Expected", "Actual", "Result"));
            sb.append("-------------------------------------------------------------------------------------------------------\n");

            for (TestResult r : results) {
                String resStr = r.passed ? "[PASS]" : "[FAIL]";
                sb.append(String.format("%-6s | %-38s | %-16s | %-16s | %-8s\n",
                        r.testId,
                        truncate(r.description, 38),
                        truncate(r.expected, 16),
                        truncate(r.actual, 16),
                        resStr));
            }

            sb.append("=======================================================================================================\n");
            sb.append(String.format("VERIFICATION SUMMARY: %d / %d Tests Passed (%.1f%%) in %d ms\n",
                    passedCount, totalCount, (passedCount * 100.0 / totalCount), durationMs));
            sb.append("=======================================================================================================\n");
            return sb.toString();
        }

        private String truncate(String val, int maxLen) {
            if (val == null) return "-";
            if (val.length() <= maxLen) return val;
            return val.substring(0, maxLen - 2) + "..";
        }
    }

    public static void main(String[] args) {
        System.out.println("Starting Automated Test Suite...");
        TestSuiteSummary summary = runAllTests();
        System.out.println(summary.getFormattedSummaryTable());
    }

    public static TestSuiteSummary runAllTests() {
        long startTime = System.currentTimeMillis();
        TestSuiteSummary summary = new TestSuiteSummary();

        // Ensure database benchmark reset first so tests run against clean data
        DatabaseResetService.resetToBenchmark();

        CustomerDAO customerDAO = new CustomerDAO();
        ApplicationWorkflowService workflowService = new ApplicationWorkflowService();

        // ---------------------------------------------------------------------
        // TC01: Personal Loan EMI Calculation (1L, 12%, 12m)
        // ---------------------------------------------------------------------
        try {
            BigDecimal principal = new BigDecimal("100000");
            BigDecimal rate = new BigDecimal("12.0");
            int tenure = 12;
            BigDecimal emi = FinancialCalculator.calculateEMI(principal, rate, tenure);
            boolean pass = emi.compareTo(new BigDecimal("8884.88")) == 0;
            summary.results.add(new TestResult("TC01", "Personal Loan EMI (1L, 12%, 12m)", "8,884.88", emi.toPlainString(), pass, "Exact reducing balance compound formula"));
        } catch (Exception e) {
            summary.results.add(new TestResult("TC01", "Personal Loan EMI (1L, 12%, 12m)", "8,884.88", e.getMessage(), false, "Exception"));
        }

        // ---------------------------------------------------------------------
        // TC02: Home / Large Loan EMI Calculation (30L, 8.5%, 240m)
        // ---------------------------------------------------------------------
        try {
            BigDecimal principal = new BigDecimal("3000000");
            BigDecimal rate = new BigDecimal("8.5");
            int tenure = 240;
            BigDecimal emi = FinancialCalculator.calculateEMI(principal, rate, tenure);
            boolean pass = emi.compareTo(new BigDecimal("26034.70")) == 0;
            summary.results.add(new TestResult("TC02", "Large Loan EMI (30L, 8.5%, 240m)", "26,034.70", emi.toPlainString(), pass, "High-tenure precision test"));
        } catch (Exception e) {
            summary.results.add(new TestResult("TC02", "Large Loan EMI (30L, 8.5%, 240m)", "26,034.70", e.getMessage(), false, "Exception"));
        }

        // ---------------------------------------------------------------------
        // TC03: FOIR Under Limit (30k income, 5k obligations)
        // ---------------------------------------------------------------------
        try {
            BigDecimal income = new BigDecimal("30000");
            BigDecimal obligations = new BigDecimal("5000");
            BigDecimal emi = new BigDecimal("8884.88");
            BigDecimal foir = FinancialCalculator.calculateFOIR(obligations, emi, income);
            boolean pass = foir.compareTo(new BigDecimal("46.28")) == 0 && foir.compareTo(new BigDecimal("50.00")) <= 0;
            summary.results.add(new TestResult("TC03", "FOIR Under Policy (30k inc, 5k debt)", "46.28% <= 50%", foir.toPlainString() + "%", pass, "Passes affordability"));
        } catch (Exception e) {
            summary.results.add(new TestResult("TC03", "FOIR Under Policy", "46.28%", e.getMessage(), false, "Exception"));
        }

        // ---------------------------------------------------------------------
        // TC04: FOIR Exceeded (30k income, 7k obligations)
        // ---------------------------------------------------------------------
        try {
            BigDecimal income = new BigDecimal("30000");
            BigDecimal obligations = new BigDecimal("7000");
            BigDecimal emi = new BigDecimal("8884.88");
            BigDecimal foir = FinancialCalculator.calculateFOIR(obligations, emi, income);
            boolean pass = foir.compareTo(new BigDecimal("52.95")) == 0 && foir.compareTo(new BigDecimal("50.00")) > 0;
            summary.results.add(new TestResult("TC04", "FOIR Exceeded (30k inc, 7k debt)", "52.95% > 50%", foir.toPlainString() + "%", pass, "Fails affordability"));
        } catch (Exception e) {
            summary.results.add(new TestResult("TC04", "FOIR Exceeded", "52.95%", e.getMessage(), false, "Exception"));
        }

        // Baseline date for age boundary tests
        LocalDate baseDate = LocalDate.of(2026, 10, 23);
        BusinessDateService.setBusinessDate(baseDate);

        // ---------------------------------------------------------------------
        // TC05: Min Age Boundary Fail (Age 20 -> Ineligible)
        // ---------------------------------------------------------------------
        try {
            Customer cAge20 = new Customer("Test Age20", baseDate.minusYears(20).minusDays(1), "9900000020", "Test Address", "PAN-AGE20");
            Long cId20 = customerDAO.createCustomer(cAge20);
            LoanApplication app20 = workflowService.createDraft(cId20, LoanProduct.PERSONAL, "BR-DEL-01", "Central Delhi", new BigDecimal("50000"), 12, new BigDecimal("30000"), BigDecimal.ZERO);
            ValidationResult res20 = workflowService.submitApplication(app20.getApplicationId(), "OFFICER_01");
            boolean pass = !res20.isEligible() && res20.getFailureReasons().stream().anyMatch(r -> r.toLowerCase().contains("age"));
            summary.results.add(new TestResult("TC05", "Min Age Limit (Age 20 -> Ineligible)", "REJECTED", pass ? "REJECTED" : "ACCEPTED", pass, "Underage rejection enforced"));
        } catch (Exception e) {
            summary.results.add(new TestResult("TC05", "Min Age Limit (Age 20)", "REJECTED", e.getMessage(), false, "Exception"));
        }

        // ---------------------------------------------------------------------
        // TC06: Min Age Boundary Pass (Age 21 -> Eligible)
        // ---------------------------------------------------------------------
        try {
            Customer cAge21 = new Customer("Test Age21", baseDate.minusYears(21), "9900000021", "Test Address", "PAN-AGE21");
            Long cId21 = customerDAO.createCustomer(cAge21);
            LoanApplication app21 = workflowService.createDraft(cId21, LoanProduct.PERSONAL, "BR-DEL-01", "Central Delhi", new BigDecimal("50000"), 12, new BigDecimal("30000"), BigDecimal.ZERO);
            ValidationResult res21 = workflowService.submitApplication(app21.getApplicationId(), "OFFICER_01");
            boolean pass = res21.isEligible();
            summary.results.add(new TestResult("TC06", "Min Age Limit (Age 21 -> Eligible)", "UNDER_REVIEW", pass ? "UNDER_REVIEW" : "REJECTED", pass, "Boundary exact pass"));
        } catch (Exception e) {
            summary.results.add(new TestResult("TC06", "Min Age Limit (Age 21)", "UNDER_REVIEW", e.getMessage(), false, "Exception"));
        }

        // ---------------------------------------------------------------------
        // TC07: Max Age Boundary Pass (Age 60 -> Eligible)
        // ---------------------------------------------------------------------
        try {
            Customer cAge60 = new Customer("Test Age60", baseDate.minusYears(60), "9900000060", "Test Address", "PAN-AGE60");
            Long cId60 = customerDAO.createCustomer(cAge60);
            LoanApplication app60 = workflowService.createDraft(cId60, LoanProduct.PERSONAL, "BR-DEL-01", "Central Delhi", new BigDecimal("50000"), 12, new BigDecimal("40000"), BigDecimal.ZERO);
            ValidationResult res60 = workflowService.submitApplication(app60.getApplicationId(), "OFFICER_01");
            boolean pass = res60.isEligible();
            summary.results.add(new TestResult("TC07", "Max Age Limit (Age 60 -> Eligible)", "UNDER_REVIEW", pass ? "UNDER_REVIEW" : "REJECTED", pass, "Boundary exact pass"));
        } catch (Exception e) {
            summary.results.add(new TestResult("TC07", "Max Age Limit (Age 60)", "UNDER_REVIEW", e.getMessage(), false, "Exception"));
        }

        // ---------------------------------------------------------------------
        // TC08: Max Age Boundary Fail (Age 61 -> Ineligible)
        // ---------------------------------------------------------------------
        try {
            Customer cAge61 = new Customer("Test Age61", baseDate.minusYears(61).minusDays(1), "9900000061", "Test Address", "PAN-AGE61");
            Long cId61 = customerDAO.createCustomer(cAge61);
            LoanApplication app61 = workflowService.createDraft(cId61, LoanProduct.PERSONAL, "BR-DEL-01", "Central Delhi", new BigDecimal("50000"), 12, new BigDecimal("40000"), BigDecimal.ZERO);
            ValidationResult res61 = workflowService.submitApplication(app61.getApplicationId(), "OFFICER_01");
            boolean pass = !res61.isEligible() && res61.getFailureReasons().stream().anyMatch(r -> r.toLowerCase().contains("age"));
            summary.results.add(new TestResult("TC08", "Max Age Limit (Age 61 -> Ineligible)", "REJECTED", pass ? "REJECTED" : "ACCEPTED", pass, "Overage rejection enforced"));
        } catch (Exception e) {
            summary.results.add(new TestResult("TC08", "Max Age Limit (Age 61)", "REJECTED", e.getMessage(), false, "Exception"));
        }

        // ---------------------------------------------------------------------
        // TC09: Duplicate Active Loan Detection
        // ---------------------------------------------------------------------
        try {
            Customer c1 = customerDAO.findById(1L).orElseThrow();
            boolean caughtDuplicate = false;
            try {
                LoanApplication appDup = workflowService.createDraft(c1.getCustomerId(), LoanProduct.PERSONAL, "BR-DEL-01", "Central Delhi", new BigDecimal("50000"), 12, new BigDecimal("50000"), BigDecimal.ZERO);
                ValidationResult resDup = workflowService.submitApplication(appDup.getApplicationId(), "OFFICER_01");
                caughtDuplicate = !resDup.isEligible();
            } catch (IllegalStateException ise) {
                // Draft creation was actively blocked due to duplicate active loan constraint
                caughtDuplicate = ise.getMessage().toLowerCase().contains("active");
            }
            summary.results.add(new TestResult("TC09", "Duplicate Active Loan Constraint", "BLOCKED", caughtDuplicate ? "BLOCKED" : "ALLOWED", caughtDuplicate, "Blocked concurrent active loan"));
        } catch (Exception e) {
            summary.results.add(new TestResult("TC09", "Duplicate Active Loan", "BLOCKED", e.getMessage(), false, "Exception"));
        }

        // ---------------------------------------------------------------------
        // TC10: ACID Controlled Rollback
        // ---------------------------------------------------------------------
        try {
            boolean rollbackSuccess = workflowService.demonstrateControlledRollback(1L);
            summary.results.add(new TestResult("TC10", "Transaction Rollback Integrity", "Rolled Back", rollbackSuccess ? "Rolled Back" : "Corrupted", rollbackSuccess, "Zero data corruption on failure"));
        } catch (Exception e) {
            summary.results.add(new TestResult("TC10", "Transaction Rollback", "Rolled Back", e.getMessage(), false, "Exception"));
        }

        // ---------------------------------------------------------------------
        // TC11: Overdue Review Aging Simulation (2 days vs 3 days)
        // ---------------------------------------------------------------------
        try {
            // Benchmark application 1 was submitted on 2026-10-23
            // Advance to 2026-10-26 (3 calendar days -> > 2 days -> Overdue)
            BusinessDateService.setBusinessDate(LocalDate.of(2026, 10, 26));
            OverdueReviewUtility watchdog = new OverdueReviewUtility();
            OverdueReviewUtility.UtilityRunResult runRes = watchdog.executeUtility();
            boolean pass = runRes.alertsCreated > 0;
            summary.results.add(new TestResult("TC11", "Overdue Aging Simulation (+3 Days)", "Alert Generated", pass ? "Alert Generated" : "No Alert", pass, "Identifies reviews older than 2 days"));
        } catch (Exception e) {
            summary.results.add(new TestResult("TC11", "Overdue Aging Simulation", "Alert Generated", e.getMessage(), false, "Exception"));
        }

        // ---------------------------------------------------------------------
        // TC12: Background Daemon Idempotency (No duplicate alerts on re-run)
        // ---------------------------------------------------------------------
        try {
            OverdueReviewUtility watchdog = new OverdueReviewUtility();
            OverdueReviewUtility.UtilityRunResult run2 = watchdog.executeUtility();
            // Second run should create 0 new alerts for the already alerted application
            boolean pass = run2.alertsCreated == 0;
            summary.results.add(new TestResult("TC12", "Daemon Idempotency (Repeat Run)", "0 Duplicates", run2.alertsCreated + " New Alerts", pass, "Composite unique constraint verified"));
        } catch (Exception e) {
            summary.results.add(new TestResult("TC12", "Daemon Idempotency", "0 Duplicates", e.getMessage(), false, "Exception"));
        }

        summary.totalCount = summary.results.size();
        summary.passedCount = (int) summary.results.stream().filter(r -> r.passed).count();
        summary.durationMs = System.currentTimeMillis() - startTime;

        // Reset business date back to baseline
        BusinessDateService.setBusinessDate(LocalDate.of(2026, 10, 23));

        return summary;
    }
}
