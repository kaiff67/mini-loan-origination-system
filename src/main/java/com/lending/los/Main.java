package com.lending.los;

import com.lending.los.dao.DBConnection;
import com.lending.los.service.BusinessDateService;
import com.lending.los.ui.ConsoleMenu;
import com.lending.los.ui.SwingDashboard;
import com.lending.los.utility.CoreDataPushUtility;
import com.lending.los.utility.OverdueReviewUtility;

import javax.swing.SwingUtilities;
import java.time.LocalDate;

/**
 * Application Entry Point.
 *
 * Demonstrates:
 * - Pure Java architecture
 * - JDBC connectivity verification
 * - Scheduled background utilities (ScheduledExecutorService)
 * - User-Friendly Desktop GUI (Java Swing)
 * - Interactive Console UI
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("               STARTING MINI LOAN ORIGINATION SYSTEM");
        System.out.println("================================================================================");

        // 1. Initialize business date (defaults to today)
        BusinessDateService.setBusinessDate(LocalDate.now());

        // 2. Test JDBC Connection
        System.out.print("[INFO] Checking Database Connectivity (" + DBConnection.getUrl() + ")... ");
        if (DBConnection.testConnection()) {
            System.out.println("CONNECTED ✅");
        } else {
            System.out.println("FAILED ❌");
            System.err.println("[ERROR] Could not connect to MySQL database.");
            System.err.println("[HINT] Ensure MySQL is running and check 'src/main/resources/db.properties'.");
            System.out.println("\nContinuing in offline/demonstration mode...\n");
        }

        // 3. Start Background Utilities
        OverdueReviewUtility overdueUtility = new OverdueReviewUtility();
        overdueUtility.startScheduler(5); // Runs every 5 minutes in background

        CoreDataPushUtility corePushUtility = new CoreDataPushUtility();
        corePushUtility.startContinuousWorker(10); // Polling every 10 seconds

        // 4. Register Shutdown Hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SHUTDOWN] Stopping background utility workers...");
            overdueUtility.stopScheduler();
            corePushUtility.stopWorker();
            System.out.println("[SHUTDOWN] Cleanup complete.");
        }));

        // 5. Launch Swing Desktop Dashboard (Native GUI)
        try {
            SwingUtilities.invokeLater(() -> {
                try {
                    SwingDashboard gui = new SwingDashboard(overdueUtility, corePushUtility);
                    gui.setVisible(true);
                    System.out.println("[UI] Desktop Window (Java Swing) opened successfully ✅");
                } catch (Exception e) {
                    System.err.println("[UI WARN] Could not launch graphical desktop window: " + e.getMessage());
                }
            });
        } catch (Exception ignored) {}

        // 6. Also start Console Menu for terminal-based testing
        ConsoleMenu console = new ConsoleMenu(overdueUtility, corePushUtility);
        console.start();
    }
}
