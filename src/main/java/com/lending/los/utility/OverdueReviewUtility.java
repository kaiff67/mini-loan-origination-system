package com.lending.los.utility;

import com.lending.los.dao.DBConnection;
import com.lending.los.dao.SystemAlertDAO;
import com.lending.los.model.SystemAlert;
import com.lending.los.service.BusinessDateService;
import com.lending.los.service.FinancialCalculator;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Asynchronous background monitor that tracks loan applications exceeding review SLAs.
 * Periodically scans for applications pending review beyond 2 calendar days,
 * creates persistent system alerts, and generates operational CSV exports.
 */
public class OverdueReviewUtility {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "OverdueReviewUtility-Thread");
        t.setDaemon(true);
        return t;
    });

    private final SystemAlertDAO alertDAO = new SystemAlertDAO();
    private static final String CSV_DIR = "reports_export";

    public void startScheduler(long intervalMinutes) {
        scheduler.scheduleAtFixedRate(this::executeUtility, 5, intervalMinutes * 60, TimeUnit.SECONDS);
        System.out.println("[UTILITY] OverdueReviewUtility scheduled to run every " + intervalMinutes + " minute(s).");
    }

    public void stopScheduler() {
        scheduler.shutdown();
    }

    /**
     * Executes the utility on-demand or as scheduled.
     */
    public synchronized UtilityRunResult executeUtility() {
        LocalDateTime startTime = BusinessDateService.getBusinessDateTime();
        LocalDate businessDate = BusinessDateService.getBusinessDate();
        int applicationsProcessed = 0;
        int alertsCreated = 0;
        String errorMessage = null;
        File exportedCsv = null;

        String querySql = "SELECT a.application_id, a.application_number, c.name AS customer_name, c.mobile, " +
                          "       a.product, a.requested_amount, a.proposed_emi, a.submitted_at " +
                          "FROM loan_application a " +
                          "JOIN customer c ON a.customer_id = c.customer_id " +
                          "WHERE a.status = 'UNDER_REVIEW'";

        try {
            File dir = new File(CSV_DIR);
            if (!dir.exists()) dir.mkdirs();

            String csvFileName = "pending_reviews_" + businessDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";
            exportedCsv = new File(dir, csvFileName);

            try (Connection conn = DBConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(querySql);
                 PrintWriter csvWriter = new PrintWriter(new FileWriter(exportedCsv))) {

                // CSV Header
                csvWriter.println("ApplicationID,ApplicationNumber,CustomerName,Mobile,Product,RequestedAmount,ProposedEMI,SubmittedAt,DaysWaiting,OverdueFlag");

                while (rs.next()) {
                    applicationsProcessed++;
                    long appId = rs.getLong("application_id");
                    String appNum = rs.getString("application_number");
                    String custName = rs.getString("customer_name");
                    String mobile = rs.getString("mobile");
                    String product = rs.getString("product");
                    BigDecimal reqAmount = rs.getBigDecimal("requested_amount");
                    BigDecimal proposedEmi = rs.getBigDecimal("proposed_emi");
                    Timestamp submittedTs = rs.getTimestamp("submitted_at");

                    LocalDate submittedDate = (submittedTs != null) ? submittedTs.toLocalDateTime().toLocalDate() : businessDate;
                    long daysWaiting = ChronoUnit.DAYS.between(submittedDate, businessDate);

                    boolean isOverdue = daysWaiting > 2;
                    String overdueFlag = isOverdue ? "Yes" : "No";

                    if (isOverdue) {
                        SystemAlert alert = new SystemAlert(appId, "REVIEW_OVERDUE", BusinessDateService.getBusinessDateTime());
                        boolean created = alertDAO.createAlertIfNotExists(alert);
                        if (created) {
                            alertsCreated++;
                        }
                    }

                    // Write CSV row
                    csvWriter.printf("%d,%s,\"%s\",%s,%s,%.2f,%.2f,%s,%d,%s%n",
                            appId, appNum, custName, mobile, product, reqAmount, proposedEmi,
                            (submittedTs != null ? submittedTs.toString() : ""),
                            daysWaiting, overdueFlag);
                }
            }

            logUtilityRun("OVERDUE_REVIEW_UTILITY", startTime, BusinessDateService.getBusinessDateTime(), "SUCCESS",
                    applicationsProcessed, alertsCreated, null);

        } catch (Exception e) {
            errorMessage = e.getMessage();
            logUtilityRun("OVERDUE_REVIEW_UTILITY", startTime, BusinessDateService.getBusinessDateTime(), "FAILED",
                    applicationsProcessed, alertsCreated, errorMessage);
            System.err.println("[UTILITY ERROR] " + errorMessage);
        }

        return new UtilityRunResult("OVERDUE_REVIEW_UTILITY", applicationsProcessed, alertsCreated, exportedCsv, errorMessage);
    }

    private void logUtilityRun(String utilityName, LocalDateTime start, LocalDateTime end, String result,
                               int processed, int alerts, String errorMsg) {
        String sql = "INSERT INTO utility_run (utility_name, started_at, completed_at, result, applications_processed, alerts_created, error_message) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, utilityName);
            ps.setTimestamp(2, Timestamp.valueOf(start));
            ps.setTimestamp(3, Timestamp.valueOf(end));
            ps.setString(4, result);
            ps.setInt(5, processed);
            ps.setInt(6, alerts);
            ps.setString(7, errorMsg);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public static class UtilityRunResult {
        public final String utilityName;
        public final int applicationsProcessed;
        public final int alertsCreated;
        public final File csvFile;
        public final String errorMessage;

        public UtilityRunResult(String utilityName, int applicationsProcessed, int alertsCreated, File csvFile, String errorMessage) {
            this.utilityName = utilityName;
            this.applicationsProcessed = applicationsProcessed;
            this.alertsCreated = alertsCreated;
            this.csvFile = csvFile;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return errorMessage == null;
        }
    }
}
