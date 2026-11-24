package com.lending.los.ui;

import com.lending.los.dao.*;
import com.lending.los.model.*;
import com.lending.los.service.*;
import com.lending.los.utility.CoreDataPushUtility;
import com.lending.los.utility.OverdueReviewUtility;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * User-friendly, clean Desktop GUI built with pure standard Java Swing.
 * Designed for effortless evaluation demonstrations.
 */
public class SwingDashboard extends JFrame {

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final LoanApplicationDAO loanDAO = new LoanApplicationDAO();
    private final ReportDAO reportDAO = new ReportDAO();
    private final ApplicationWorkflowService workflowService = new ApplicationWorkflowService();
    private final OverdueReviewUtility overdueUtility;
    private final CoreDataPushUtility corePushUtility;

    // Header date label
    private JLabel lblCurrentDate;

    // Tab 1 Components
    private JTable tblApplications;
    private DefaultTableModel modelApplications;

    // Tab 3 Components (Reports)
    private JComboBox<String> cmbReportSelect;
    private JTable tblReportOutput;
    private DefaultTableModel modelReportOutput;

    public SwingDashboard(OverdueReviewUtility overdueUtility, CoreDataPushUtility corePushUtility) {
        this.overdueUtility = overdueUtility;
        this.corePushUtility = corePushUtility;

        setTitle("Mini Loan Origination System (LOS) - Desktop Dashboard");
        setSize(1100, 720);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Apply native system Look and Feel for clean modern look
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        initUI();
        refreshApplicationsTable();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        root.setBackground(new Color(245, 247, 250));

        // 1. Top Header Banner
        root.add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Main Tabbed Navigation
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabbedPane.addTab("  📊 1. All Loans & Actions  ", createApplicationsTab());
        tabbedPane.addTab("  ➕ 2. Apply for New Loan  ", createNewLoanTab());
        tabbedPane.addTab("  📈 3. Six Bank Reports  ", createReportsTab());
        tabbedPane.addTab("  ⚙️ 4. Background Robots & Demos  ", createUtilitiesTab());

        root.add(tabbedPane, BorderLayout.CENTER);

        setContentPane(root);
    }

    // -------------------------------------------------------------------------
    // Top Banner
    // -------------------------------------------------------------------------
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(24, 43, 73));
        header.setBorder(new EmptyBorder(14, 18, 14, 18));

        // Left title
        JLabel lblTitle = new JLabel("🏦 Mini Loan Origination System (LOS)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("End-to-End Digital Lending Workflow • Automated Underwriting • Pure Java & JDBC");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(180, 205, 237));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 2, 2));
        titlePanel.setOpaque(false);
        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);
        header.add(titlePanel, BorderLayout.WEST);

        // Right date badge
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        datePanel.setOpaque(false);

        lblCurrentDate = new JLabel("📅 Date: " + BusinessDateService.getFormattedBusinessDate());
        lblCurrentDate.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCurrentDate.setForeground(Color.WHITE);

        JButton btnChangeDate = new JButton("Change Date");
        btnChangeDate.setFocusPainted(false);
        btnChangeDate.addActionListener(e -> promptChangeDate());

        datePanel.add(lblCurrentDate);
        datePanel.add(btnChangeDate);
        header.add(datePanel, BorderLayout.EAST);

        return header;
    }

    // -------------------------------------------------------------------------
    // Tab 1: All Loans & Quick Actions
    // -------------------------------------------------------------------------
    private JPanel createApplicationsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Table
        String[] columns = {"ID", "App Number", "Customer Name", "Product", "Amount (INR)", "Monthly EMI", "FOIR %", "Current Stage", "Submitted Date"};
        modelApplications = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblApplications = new JTable(modelApplications);
        tblApplications.setRowHeight(28);
        tblApplications.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblApplications.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tblApplications.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Center align numerical IDs and amounts
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tblApplications.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tblApplications.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        tblApplications.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);
        tblApplications.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(tblApplications);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Action Buttons Bar
        JPanel actionsBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        actionsBar.setBackground(Color.WHITE);
        actionsBar.setBorder(BorderFactory.createTitledBorder("Step-by-Step Actions for Selected Loan:"));

        JButton btnSubmit = new JButton("1. ⚡ Check Eligibility & Submit");
        btnSubmit.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSubmit.setToolTipText("Submits DRAFT loan. Runs automated checks (Age, FOIR <= 50%). Moves to UNDER_REVIEW or REJECTED.");
        btnSubmit.addActionListener(e -> handleActionSubmit());

        JButton btnReview = new JButton("2. 👔 Manager Review (Approve / Reject)");
        btnReview.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnReview.setToolTipText("Credit Officer decision for UNDER_REVIEW loan. Requires reason if rejected.");
        btnReview.addActionListener(e -> handleActionReview());

        JButton btnDoc = new JButton("3. 📄 Generate Official Form");
        btnDoc.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDoc.setToolTipText("Generates physical stamped Application Form document on disk.");
        btnDoc.addActionListener(e -> handleActionGenerateDoc());

        JButton btnDisburse = new JButton("4. 💰 Disburse Money");
        btnDisburse.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDisburse.setToolTipText("Disburses approved loan atomically with unique reference number.");
        btnDisburse.addActionListener(e -> handleActionDisburse());

        JButton btnRefresh = new JButton("🔄 Refresh List");
        btnRefresh.addActionListener(e -> refreshApplicationsTable());

        actionsBar.add(btnSubmit);
        actionsBar.add(btnReview);
        actionsBar.add(btnDoc);
        actionsBar.add(btnDisburse);
        actionsBar.add(btnRefresh);

        panel.add(actionsBar, BorderLayout.SOUTH);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Tab 2: Apply for a New Loan
    // -------------------------------------------------------------------------
    private JPanel createNewLoanTab() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 15, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Left Card: Customer Details
        JPanel custCard = new JPanel(new GridBagLayout());
        custCard.setBackground(Color.WHITE);
        custCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 235), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        JLabel lblCustTitle = new JLabel("Step A: Register Customer (Borrower)");
        lblCustTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblCustTitle.setForeground(new Color(24, 43, 73));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        custCard.add(lblCustTitle, gbc);

        gbc.gridwidth = 1;

        JTextField txtName = new JTextField("Amit Kumar");
        JTextField txtDob = new JTextField("1999-07-20");
        JTextField txtMobile = new JTextField("9812345670");
        JTextField txtAddress = new JTextField("Sector 62, Noida, UP");
        JTextField txtKyc = new JTextField("PAN-ABCDE5566G");

        addFormField(custCard, gbc, 1, "Full Name:", txtName);
        addFormField(custCard, gbc, 2, "Date of Birth (YYYY-MM-DD):", txtDob);
        addFormField(custCard, gbc, 3, "10-Digit Mobile:", txtMobile);
        addFormField(custCard, gbc, 4, "Home Address:", txtAddress);
        addFormField(custCard, gbc, 5, "KYC Identifier (PAN / Aadhaar):", txtKyc);

        JButton btnSaveCust = new JButton("💾 Save Customer Profile");
        btnSaveCust.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSaveCust.setBackground(new Color(33, 115, 70));
        btnSaveCust.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        custCard.add(btnSaveCust, gbc);

        // Right Card: Loan Application Details
        JPanel loanCard = new JPanel(new GridBagLayout());
        loanCard.setBackground(Color.WHITE);
        loanCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 235), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.fill = GridBagConstraints.HORIZONTAL;
        gbcL.insets = new Insets(6, 6, 6, 6);

        JLabel lblLoanTitle = new JLabel("Step B: Create Loan Application");
        lblLoanTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblLoanTitle.setForeground(new Color(24, 43, 73));
        gbcL.gridx = 0; gbcL.gridy = 0; gbcL.gridwidth = 2;
        loanCard.add(lblLoanTitle, gbcL);

        gbcL.gridwidth = 1;

        JTextField txtCustId = new JTextField("1");
        JComboBox<String> cmbProduct = new JComboBox<>(new String[]{"PERSONAL (12% interest, up to 5 Lakh)", "EDUCATION (10% interest, up to 10 Lakh)"});
        JTextField txtAmount = new JTextField("100000");
        JTextField txtTenure = new JTextField("12");
        JTextField txtIncome = new JTextField("30000");
        JTextField txtDebts = new JTextField("5000");

        addFormField(loanCard, gbcL, 1, "Customer ID:", txtCustId);
        addFormField(loanCard, gbcL, 2, "Loan Product:", cmbProduct);
        addFormField(loanCard, gbcL, 3, "Requested Amount (INR):", txtAmount);
        addFormField(loanCard, gbcL, 4, "Tenure (Months):", txtTenure);
        addFormField(loanCard, gbcL, 5, "Monthly Net Salary (INR):", txtIncome);
        addFormField(loanCard, gbcL, 6, "Existing Monthly EMIs (INR):", txtDebts);

        // Preview calculation button
        JButton btnPreview = new JButton("🔍 Calculate EMI & Affordability Preview");
        btnPreview.addActionListener(e -> {
            try {
                BigDecimal amt = new BigDecimal(txtAmount.getText().trim());
                int n = Integer.parseInt(txtTenure.getText().trim());
                BigDecimal inc = new BigDecimal(txtIncome.getText().trim());
                BigDecimal debts = new BigDecimal(txtDebts.getText().trim());
                LoanProduct prod = cmbProduct.getSelectedIndex() == 0 ? LoanProduct.PERSONAL : LoanProduct.EDUCATION;

                BigDecimal emi = FinancialCalculator.calculateEMI(amt, prod.getAnnualInterestRate(), n);
                BigDecimal foir = FinancialCalculator.calculateFOIR(debts, emi, inc);

                String msg = String.format("<html><b>Calculation Preview:</b><br><br>" +
                                "• Proposed Monthly EMI: <b>INR %s</b><br>" +
                                "• Total Monthly Obligations: INR %s<br>" +
                                "• Calculated FOIR: <b>%s%%</b> (Policy Limit: &le; 50.00%%)<br>" +
                                "• Likely Result: <b><font color='%s'>%s</font></b></html>",
                        FinancialCalculator.formatIndianCurrency(emi),
                        FinancialCalculator.formatIndianCurrency(debts.add(emi)),
                        foir,
                        (foir.compareTo(new BigDecimal("50.00")) <= 0 ? "green" : "red"),
                        (foir.compareTo(new BigDecimal("50.00")) <= 0 ? "PASSED (Eligible)" : "FAILED (Exceeds 50% threshold)"));

                JOptionPane.showMessageDialog(this, msg, "Financial Calculation Preview", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numerical inputs: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        gbcL.gridx = 0; gbcL.gridy = 7; gbcL.gridwidth = 2;
        loanCard.add(btnPreview, gbcL);

        JButton btnCreateLoan = new JButton("🚀 Submit Loan Application");
        btnCreateLoan.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnCreateLoan.setBackground(new Color(24, 43, 73));
        btnCreateLoan.setForeground(Color.WHITE);
        btnCreateLoan.addActionListener(e -> {
            try {
                Long cId = Long.parseLong(txtCustId.getText().trim());
                BigDecimal amt = new BigDecimal(txtAmount.getText().trim());
                int n = Integer.parseInt(txtTenure.getText().trim());
                BigDecimal inc = new BigDecimal(txtIncome.getText().trim());
                BigDecimal debts = new BigDecimal(txtDebts.getText().trim());
                LoanProduct prod = cmbProduct.getSelectedIndex() == 0 ? LoanProduct.PERSONAL : LoanProduct.EDUCATION;

                LoanApplication app = workflowService.createDraft(cId, prod, "BR-DEL-01", "Central Delhi Branch",
                        amt, n, inc, debts);

                refreshApplicationsTable();
                JOptionPane.showMessageDialog(this, "Loan application created successfully!\nApplication Number: " +
                        app.getApplicationNumber() + "\nStatus: DRAFT", "Application Created", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error creating loan: " + ex.getMessage(), "Validation Failure", JOptionPane.ERROR_MESSAGE);
            }
        });

        gbcL.gridx = 0; gbcL.gridy = 8; gbcL.gridwidth = 2;
        loanCard.add(btnCreateLoan, gbcL);

        // Wire Save Customer Profile
        btnSaveCust.addActionListener(e -> {
            try {
                String name = txtName.getText().trim();
                LocalDate dob = LocalDate.parse(txtDob.getText().trim());
                String mob = txtMobile.getText().trim();
                String addr = txtAddress.getText().trim();
                String kyc = txtKyc.getText().trim();

                Customer c = new Customer(name, dob, mob, addr, kyc);
                c.setCreatedAt(BusinessDateService.getBusinessDateTime());
                Long newId = customerDAO.createCustomer(c);

                txtCustId.setText(String.valueOf(newId));
                JOptionPane.showMessageDialog(this, "Customer saved with ID: " + newId + "\nCustomer ID filled automatically in Step B!", "Customer Registered", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving customer: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(custCard);
        panel.add(loanCard);
        return panel;
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.gridy = row;
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(field, gbc);
    }

    // -------------------------------------------------------------------------
    // Tab 3: Six SQL Analytics Reports
    // -------------------------------------------------------------------------
    private JPanel createReportsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));

        JLabel lblChoose = new JLabel("Choose Bank Report:");
        lblChoose.setFont(new Font("Segoe UI", Font.BOLD, 13));

        cmbReportSelect = new JComboBox<>(new String[]{
                "Report 1: Application Register (All loans by product & status)",
                "Report 2: Product Pipeline (Counts and sums grouped by product & branch)",
                "Report 3: Money Disbursed (Date-wise & branch-wise totals)",
                "Report 4: Overdue Reviews (> 2 Calendar Days waiting)",
                "Report 5: Customers who applied but got NO disbursement",
                "Report 6: Repeat Applicants (Borrowers with > 1 loan application)"
        });
        cmbReportSelect.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JButton btnRunReport = new JButton("🔍 Generate Report");
        btnRunReport.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRunReport.setBackground(new Color(24, 43, 73));
        btnRunReport.setForeground(Color.WHITE);
        btnRunReport.addActionListener(e -> executeSelectedReport());

        topBar.add(lblChoose);
        topBar.add(cmbReportSelect);
        topBar.add(btnRunReport);

        panel.add(topBar, BorderLayout.NORTH);

        // Report table output
        modelReportOutput = new DefaultTableModel();
        tblReportOutput = new JTable(modelReportOutput);
        tblReportOutput.setRowHeight(26);
        tblReportOutput.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblReportOutput.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        panel.add(new JScrollPane(tblReportOutput), BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Tab 4: Background Robots & Simulation
    // -------------------------------------------------------------------------
    private JPanel createUtilitiesTab() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Card 1: Overdue Watchdog
        JPanel card1 = createUtilCard("⏰ Robot 1: Overdue Review Watchdog",
                "Automatically checks for loans in UNDER_REVIEW older than 2 calendar days.\n" +
                "• Creates alert in database.\n• Exports a daily CSV spreadsheet for managers.\n• Guarantees ZERO duplicate alerts.",
                "▶ Run Overdue Watchdog Now", () -> {
                    OverdueReviewUtility.UtilityRunResult res = overdueUtility.executeUtility();
                    String msg = "Watchdog Run Finished!\n" +
                            "• Applications Scanned: " + res.applicationsProcessed + "\n" +
                            "• Overdue Alerts Created: " + res.alertsCreated + "\n" +
                            (res.csvFile != null ? "• CSV Spreadsheet Exported: " + res.csvFile.getName() : "");
                    JOptionPane.showMessageDialog(this, msg, "Overdue Review Result", JOptionPane.INFORMATION_MESSAGE);
                });

        // Card 2: Core Banking Sync
        JPanel card2 = createUtilCard("🔄 Robot 2: Core Banking Synchronizer",
                "Continuously running background worker.\n" +
                "• Grabs all loans that completed DISBURSEMENT.\n" +
                "• Pushes applicant and loan details into external table NG_CORE_BANKING.\n" +
                "• Moves loan stage to EXIT automatically.",
                "▶ Run Core Banking Push Now", () -> {
                    int count = corePushUtility.pushDisbursedCasesToCoreBanking();
                    refreshApplicationsTable();
                    JOptionPane.showMessageDialog(this, "Core Banking Push Finished!\n" + count + " case(s) pushed to Core Banking table and transitioned to EXIT.", "Core Banking Sync", JOptionPane.INFORMATION_MESSAGE);
                });

        // Card 3: Date Simulation
        JPanel card3 = createUtilCard("📅 Examiner Date Simulation",
                "Allows advancing business date to demonstrate review aging without waiting in real-time.\n" +
                "• Test: 2 days old = Not Overdue.\n" +
                "• Test: 3 days old = Overdue alert raised!",
                "⏩ Advance Date by +3 Days", () -> {
                    BusinessDateService.advanceDays(3);
                    lblCurrentDate.setText("📅 Date: " + BusinessDateService.getFormattedBusinessDate());
                    JOptionPane.showMessageDialog(this, "Business date advanced by +3 days!\nNew simulated date: " +
                            BusinessDateService.getFormattedBusinessDate() + "\nNow run Robot 1 to see overdue loans flagged!", "Date Advanced", JOptionPane.INFORMATION_MESSAGE);
                });

        // Card 4: Rollback Test (Scenario 09)
        JPanel card4 = createUtilCard("🛡️ Scenario 09: Transaction Rollback Demo",
                "Demonstrates enterprise ACID transaction protection.\n" +
                "• Intentionally simulates a database error during a multi-table update.\n" +
                "• Proves that zero corrupt or partial data is committed.",
                "🧪 Test Controlled Rollback", () -> {
                    boolean ok = workflowService.demonstrateControlledRollback(1L);
                    if (ok) {
                        JOptionPane.showMessageDialog(this, "SCENARIO 09 VERIFIED PASSED!\n" +
                                "Simulated database failure caught successfully.\n" +
                                "Explicit rollback executed. Zero records corrupted!", "Transaction Rollback Verified", JOptionPane.INFORMATION_MESSAGE);
                    }
                });

        panel.add(card1);
        panel.add(card2);
        panel.add(card3);
        panel.add(card4);
        return panel;
    }

    private JPanel createUtilCard(String title, String desc, String btnText, Runnable action) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 235), 1),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(new Color(24, 43, 73));

        JTextArea txtDesc = new JTextArea(desc);
        txtDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtDesc.setForeground(new Color(80, 80, 80));
        txtDesc.setEditable(false);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        txtDesc.setOpaque(false);

        JButton btn = new JButton(btnText);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.addActionListener(e -> action.run());

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(txtDesc, BorderLayout.CENTER);
        card.add(btn, BorderLayout.SOUTH);
        return card;
    }

    // -------------------------------------------------------------------------
    // Workflow Action Handlers
    // -------------------------------------------------------------------------
    private LoanApplication getSelectedApplication() {
        int row = tblApplications.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please click on a loan from the table first!", "No Loan Selected", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        Long appId = (Long) modelApplications.getValueAt(row, 0);
        try {
            return loanDAO.findById(appId).orElse(null);
        } catch (SQLException e) {
            return null;
        }
    }

    private void handleActionSubmit() {
        LoanApplication app = getSelectedApplication();
        if (app == null) return;

        if (app.getStatus() != ApplicationStage.DRAFT) {
            JOptionPane.showMessageDialog(this, "Only DRAFT loans can be submitted!\nCurrent status is: " + app.getStatus(), "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            ValidationResult res = workflowService.submitApplication(app.getApplicationId(), "LOAN_OFFICER_01");
            refreshApplicationsTable();

            if (res.isEligible()) {
                JOptionPane.showMessageDialog(this, "✅ APPROVED BY AUTO-ENGINE!\n" +
                        "• Monthly EMI: INR " + FinancialCalculator.formatIndianCurrency(res.getCalculatedEmi()) + "\n" +
                        "• FOIR: " + res.getCalculatedFoir() + "% (Policy <= 50.00%)\n" +
                        "• Status moved to: UNDER_REVIEW", "Automated Underwriting", JOptionPane.INFORMATION_MESSAGE);
            } else {
                StringBuilder sb = new StringBuilder("❌ REJECTED BY AUTO-ENGINE:\n\n");
                for (String reason : res.getFailureReasons()) {
                    sb.append("• ").append(reason).append("\n");
                }
                sb.append("\nStatus moved to: REJECTED");
                JOptionPane.showMessageDialog(this, sb.toString(), "Ineligible Application", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Submission error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleActionReview() {
        LoanApplication app = getSelectedApplication();
        if (app == null) return;

        if (app.getStatus() != ApplicationStage.UNDER_REVIEW) {
            JOptionPane.showMessageDialog(this, "Only UNDER_REVIEW loans can be reviewed by a manager!\nCurrent status is: " + app.getStatus(), "Invalid Action", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] options = {"👍 Approve Loan", "👎 Reject Loan", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
                "Select Reviewer Decision for Loan " + app.getApplicationNumber() + ":",
                "Manager Review Decision", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice == 0) { // Approve
            try {
                workflowService.reviewDecision(app.getApplicationId(), true, "Approved by credit manager", "REV_PRIYA");
                refreshApplicationsTable();
                JOptionPane.showMessageDialog(this, "Loan approved! Status moved to CREDIT_APPROVAL.", "Approved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else if (choice == 1) { // Reject
            String reason = JOptionPane.showInputDialog(this, "Enter mandatory rejection reason:", "Rejection Reason", JOptionPane.WARNING_MESSAGE);
            if (reason != null && !reason.trim().isEmpty()) {
                try {
                    workflowService.reviewDecision(app.getApplicationId(), false, reason, "REV_PRIYA");
                    refreshApplicationsTable();
                    JOptionPane.showMessageDialog(this, "Loan rejected with reason. Status moved to REJECTED.", "Rejected", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else if (reason != null) {
                JOptionPane.showMessageDialog(this, "Rejection reason cannot be blank!", "Mandatory Input", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleActionGenerateDoc() {
        LoanApplication app = getSelectedApplication();
        if (app == null) return;

        if (app.getStatus() != ApplicationStage.CREDIT_APPROVAL) {
            JOptionPane.showMessageDialog(this, "Official Form can only be generated when loan is in CREDIT_APPROVAL status.\nCurrent status is: " + app.getStatus(), "Action Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            File doc = workflowService.performCreditApproval(app.getApplicationId(), "CREDIT_HEAD_01");
            JOptionPane.showMessageDialog(this, "📄 Official Application Form successfully generated and sealed!\n" +
                    "File saved at:\n" + doc.getAbsolutePath(), "Form Generated", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleActionDisburse() {
        LoanApplication app = getSelectedApplication();
        if (app == null) return;

        if (app.getStatus() != ApplicationStage.CREDIT_APPROVAL) {
            JOptionPane.showMessageDialog(this, "Only CREDIT_APPROVAL loans can be disbursed!\nCurrent status is: " + app.getStatus(), "Action Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String ref = JOptionPane.showInputDialog(this, "Enter unique disbursement reference number:", "DISB-" + System.currentTimeMillis());
        if (ref != null && !ref.trim().isEmpty()) {
            try {
                Disbursement d = workflowService.disburseLoan(app.getApplicationId(), ref.trim(), "DISB_OFFICER_01");
                refreshApplicationsTable();
                JOptionPane.showMessageDialog(this, "💰 Loan Disbursed Successfully!\n" +
                        "• Amount: INR " + FinancialCalculator.formatIndianCurrency(d.getAmount()) + "\n" +
                        "• Reference: " + d.getReferenceNumber() + "\n" +
                        "• Status moved to: DISBURSEMENT\n\n" +
                        "(Robot 2 will automatically sync this to Core Banking and move to EXIT)", "Disbursement Completed", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Disbursement failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void promptChangeDate() {
        String input = JOptionPane.showInputDialog(this, "Enter new Business Date (YYYY-MM-DD):", BusinessDateService.getBusinessDate().toString());
        if (input != null && !input.trim().isEmpty()) {
            try {
                LocalDate d = LocalDate.parse(input.trim());
                BusinessDateService.setBusinessDate(d);
                lblCurrentDate.setText("📅 Date: " + BusinessDateService.getFormattedBusinessDate());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Use YYYY-MM-DD", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void refreshApplicationsTable() {
        modelApplications.setRowCount(0);
        try {
            List<LoanApplication> list = loanDAO.findAll();
            for (LoanApplication a : list) {
                String subDate = a.getSubmittedAt() != null ? a.getSubmittedAt().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")) : "-";
                modelApplications.addRow(new Object[]{
                        a.getApplicationId(),
                        a.getApplicationNumber(),
                        a.getCustomerName(),
                        a.getProduct(),
                        FinancialCalculator.formatIndianCurrency(a.getRequestedAmount()),
                        FinancialCalculator.formatIndianCurrency(a.getProposedEmi()),
                        a.getFoirPercentage() != null ? a.getFoirPercentage() + "%" : "-",
                        a.getStatus(),
                        subDate
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching loans: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void executeSelectedReport() {
        int idx = cmbReportSelect.getSelectedIndex();
        modelReportOutput.setRowCount(0);
        modelReportOutput.setColumnCount(0);

        try (java.sql.Connection conn = DBConnection.getConnection()) {
            java.sql.Statement stmt = conn.createStatement();
            java.sql.ResultSet rs = null;

            switch (idx) {
                case 0 -> { // Report 1
                    rs = stmt.executeQuery("SELECT a.application_number, c.name, a.product, a.requested_amount, a.proposed_emi, a.status, a.submitted_at " +
                            "FROM loan_application a JOIN customer c ON a.customer_id = c.customer_id ORDER BY a.created_at DESC");
                    modelReportOutput.setColumnIdentifiers(new String[]{"App Number", "Customer", "Product", "Amount (INR)", "EMI (INR)", "Status", "Submitted Date"});
                    while (rs.next()) {
                        modelReportOutput.addRow(new Object[]{
                                rs.getString(1), rs.getString(2), rs.getString(3),
                                FinancialCalculator.formatIndianCurrency(rs.getBigDecimal(4)),
                                FinancialCalculator.formatIndianCurrency(rs.getBigDecimal(5)),
                                rs.getString(6), rs.getString(7)
                        });
                    }
                }
                case 1 -> { // Report 2
                    rs = stmt.executeQuery("SELECT product, status, COUNT(*), SUM(requested_amount) FROM loan_application GROUP BY product, status");
                    modelReportOutput.setColumnIdentifiers(new String[]{"Product", "Status", "Count", "Total Requested Amount (INR)"});
                    while (rs.next()) {
                        modelReportOutput.addRow(new Object[]{
                                rs.getString(1), rs.getString(2), rs.getInt(3),
                                FinancialCalculator.formatIndianCurrency(rs.getBigDecimal(4))
                        });
                    }
                }
                case 2 -> { // Report 3
                    rs = stmt.executeQuery("SELECT DATE(disbursed_at), COUNT(*), SUM(amount) FROM disbursement GROUP BY DATE(disbursed_at)");
                    modelReportOutput.setColumnIdentifiers(new String[]{"Disbursed Date", "Count", "Total Disbursed (INR)"});
                    while (rs.next()) {
                        modelReportOutput.addRow(new Object[]{
                                rs.getString(1), rs.getInt(2),
                                FinancialCalculator.formatIndianCurrency(rs.getBigDecimal(3))
                        });
                    }
                }
                case 3 -> { // Report 4
                    String sql = "SELECT a.application_number, c.name, a.requested_amount, a.submitted_at, DATEDIFF('" +
                            BusinessDateService.getBusinessDate() + "', DATE(a.submitted_at)) AS days_waiting " +
                            "FROM loan_application a JOIN customer c ON a.customer_id = c.customer_id " +
                            "WHERE a.status = 'UNDER_REVIEW' AND DATEDIFF('" + BusinessDateService.getBusinessDate() + "', DATE(a.submitted_at)) > 2";
                    rs = stmt.executeQuery(sql);
                    modelReportOutput.setColumnIdentifiers(new String[]{"App Number", "Customer Name", "Requested Amount (INR)", "Submitted Date", "Days Waiting"});
                    while (rs.next()) {
                        modelReportOutput.addRow(new Object[]{
                                rs.getString(1), rs.getString(2),
                                FinancialCalculator.formatIndianCurrency(rs.getBigDecimal(3)),
                                rs.getString(4), rs.getInt(5)
                        });
                    }
                }
                case 4 -> { // Report 5
                    String sql = "SELECT DISTINCT c.customer_id, c.name, c.mobile FROM customer c JOIN loan_application a ON c.customer_id = a.customer_id " +
                            "WHERE c.customer_id NOT IN (SELECT DISTINCT app.customer_id FROM loan_application app JOIN disbursement d ON app.application_id = d.application_id)";
                    rs = stmt.executeQuery(sql);
                    modelReportOutput.setColumnIdentifiers(new String[]{"Customer ID", "Customer Name", "Mobile"});
                    while (rs.next()) {
                        modelReportOutput.addRow(new Object[]{rs.getLong(1), rs.getString(2), rs.getString(3)});
                    }
                }
                case 5 -> { // Report 6
                    String sql = "SELECT c.name, c.mobile, COUNT(a.application_id), SUM(a.requested_amount) " +
                            "FROM customer c JOIN loan_application a ON c.customer_id = a.customer_id " +
                            "GROUP BY c.customer_id, c.name, c.mobile HAVING COUNT(a.application_id) > 1";
                    rs = stmt.executeQuery(sql);
                    modelReportOutput.setColumnIdentifiers(new String[]{"Customer Name", "Mobile", "Number of Applications", "Total Requested (INR)"});
                    while (rs.next()) {
                        modelReportOutput.addRow(new Object[]{
                                rs.getString(1), rs.getString(2), rs.getInt(3),
                                FinancialCalculator.formatIndianCurrency(rs.getBigDecimal(4))
                        });
                    }
                }
            }
            if (rs != null) rs.close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error running report: " + ex.getMessage(), "Query Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
