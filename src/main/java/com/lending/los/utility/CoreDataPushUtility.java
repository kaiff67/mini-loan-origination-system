package com.lending.los.utility;

import com.lending.los.dao.*;
import com.lending.los.model.*;
import com.lending.los.service.BusinessDateService;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Continuous background utility that pushes disbursed loans to the external NG_CORE_BANKING table
 * and moves them to EXIT stage.
 *
 * Guarantees:
 * 1. Zero missed records.
 * 2. Zero duplicate entries in NG_CORE_BANKING (enforced by DB UNIQUE ApplicationID constraint).
 * 3. Atomic transition to EXIT and audit log.
 */
public class CoreDataPushUtility {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "CoreDataPushUtility-Thread");
        t.setDaemon(true);
        return t;
    });

    private final LoanApplicationDAO loanApplicationDAO = new LoanApplicationDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final DisbursementDAO disbursementDAO = new DisbursementDAO();
    private final CoreBankingDAO coreBankingDAO = new CoreBankingDAO();
    private final ApplicationHistoryDAO historyDAO = new ApplicationHistoryDAO();

    public void startContinuousWorker(long pollIntervalSeconds) {
        scheduler.scheduleWithFixedDelay(this::pushDisbursedCasesToCoreBanking, 2, pollIntervalSeconds, TimeUnit.SECONDS);
        System.out.println("[UTILITY] CoreDataPushUtility started in background (polling every " + pollIntervalSeconds + "s).");
    }

    public void stopWorker() {
        scheduler.shutdown();
    }

    /**
     * Executes push process on demand or periodically.
     */
    public synchronized int pushDisbursedCasesToCoreBanking() {
        int pushedCount = 0;
        try {
            // Find all applications currently in DISBURSEMENT stage
            List<LoanApplication> disbursedApps = loanApplicationDAO.findByStage(ApplicationStage.DISBURSEMENT);

            for (LoanApplication app : disbursedApps) {
                boolean success = processSingleCase(app);
                if (success) {
                    pushedCount++;
                }
            }
        } catch (Exception e) {
            System.err.println("[CORE PUSH ERROR] " + e.getMessage());
        }
        return pushedCount;
    }

    private boolean processSingleCase(LoanApplication app) {
        Connection conn = null;
        try {
            Optional<Customer> optCustomer = customerDAO.findById(app.getCustomerId());
            Optional<Disbursement> optDisbursement = disbursementDAO.findByApplicationId(app.getApplicationId());

            if (optCustomer.isEmpty() || optDisbursement.isEmpty()) {
                System.err.println("[WARN] Missing customer or disbursement record for App ID " + app.getApplicationId() + ". Skipping push.");
                return false;
            }

            Customer c = optCustomer.get();
            Disbursement d = optDisbursement.get();

            CoreBankingRecord record = new CoreBankingRecord(
                    c.getCustomerId(),
                    app.getApplicationId(),
                    c.getName(),
                    c.getAddress(),
                    c.getDateOfBirth(),
                    c.getKycDetails(),
                    app.getRequestedAmount(),
                    d.getAmount(),
                    d.getDisbursedAt()
            );

            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Push into NG_CORE_BANKING table
            boolean inserted = coreBankingDAO.pushRecord(record, conn);
            if (inserted) {
                // 2. Advance stage to EXIT
                ApplicationStage oldStage = app.getStatus();
                app.setStatus(ApplicationStage.EXIT);
                loanApplicationDAO.update(app, conn);

                // 3. Log Audit History
                historyDAO.logHistory(new ApplicationHistory(app.getApplicationId(), "CORE_BANKING_PUSH",
                        oldStage.name(), ApplicationStage.EXIT.name(), "SYSTEM_UTILITY",
                        BusinessDateService.getBusinessDateTime(), "Pushed to NG_CORE_BANKING table and transitioned to EXIT."), conn);

                conn.commit(); // Atomic commit
                System.out.println("[CORE PUSH] Application " + app.getApplicationNumber() + " successfully pushed to Core Banking -> Moved to EXIT.");
                return true;
            } else {
                conn.rollback();
                return false;
            }

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            System.err.println("[ERROR] Failed to push App ID " + app.getApplicationId() + ": " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
}
