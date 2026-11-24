package com.lending.los.ui;

import com.lending.los.dao.DBConnection;
import com.lending.los.model.ApplicationStage;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.EnumMap;
import java.util.Map;

/**
 * Modern visual bar chart rendering the live pipeline distribution across all lending stages.
 * Built strictly with pure Java AWT/Graphics2D (zero third-party dependencies).
 * Displays stage volumes, percentage shares, and distinct visual state indicators.
 */
public class PipelineChartPanel extends JPanel {

    private final Map<ApplicationStage, Integer> stageCounts = new EnumMap<>(ApplicationStage.class);
    private int totalApplications = 0;

    // Stage color palette (Executive Dark Theme)
    private static final Map<ApplicationStage, Color> STAGE_COLORS = new EnumMap<>(ApplicationStage.class);
    static {
        STAGE_COLORS.put(ApplicationStage.DRAFT, new Color(100, 116, 139));            // Slate
        STAGE_COLORS.put(ApplicationStage.UNDER_REVIEW, new Color(245, 158, 11));        // Amber Warning
        STAGE_COLORS.put(ApplicationStage.CREDIT_APPROVAL, new Color(37, 99, 235));      // Royal Blue
        STAGE_COLORS.put(ApplicationStage.DISBURSEMENT, new Color(16, 185, 129));        // Emerald Green
        STAGE_COLORS.put(ApplicationStage.CORE_BANKING_PUSH, new Color(14, 165, 233));   // Sky Blue
        STAGE_COLORS.put(ApplicationStage.EXIT, new Color(13, 148, 136));                // Deep Teal
        STAGE_COLORS.put(ApplicationStage.REJECTED, new Color(225, 29, 72));            // Crimson Red
    }

    public PipelineChartPanel() {
        setPreferredSize(new Dimension(800, 260));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        refreshData();
    }

    public void refreshData() {
        stageCounts.clear();
        for (ApplicationStage stage : ApplicationStage.values()) {
            stageCounts.put(stage, 0);
        }
        totalApplications = 0;

        String sql = "SELECT status, COUNT(*) FROM loan_application GROUP BY status";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String stageStr = rs.getString(1);
                int count = rs.getInt(2);
                try {
                    ApplicationStage stage = ApplicationStage.valueOf(stageStr);
                    stageCounts.put(stage, count);
                    totalApplications += count;
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (Exception e) {
            // Keep empty or zeroes if DB unreachable
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // 1. Header Title & Total Portfolio Badge
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.setColor(new Color(30, 41, 59));
        g2.drawString("📊 Portfolio Stage Distribution (Live Pipeline Chart)", 16, 26);

        String totalBadge = "Total Loans: " + totalApplications;
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        int badgeWidth = fm.stringWidth(totalBadge) + 16;
        g2.setColor(new Color(241, 245, 249));
        g2.fillRoundRect(width - badgeWidth - 20, 10, badgeWidth, 24, 8, 8);
        g2.setColor(new Color(71, 85, 105));
        g2.drawString(totalBadge, width - badgeWidth - 12, 26);

        // 2. Chart Layout Margins
        int chartTop = 55;
        int chartBottom = height - 45;
        int chartHeight = chartBottom - chartTop;
        int chartLeft = 50;
        int chartRight = width - 30;
        int chartWidth = chartRight - chartLeft;

        // Find max count for scaling (at least 5)
        int maxVal = 5;
        for (int c : stageCounts.values()) {
            if (c > maxVal) maxVal = c;
        }

        // Draw horizontal grid lines
        g2.setColor(new Color(241, 245, 249));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        int gridSteps = 4;
        for (int i = 0; i <= gridSteps; i++) {
            int y = chartBottom - (int) ((double) i / gridSteps * chartHeight);
            g2.drawLine(chartLeft, y, chartRight, y);
            int labelVal = (int) Math.round((double) maxVal * i / gridSteps);
            g2.setColor(new Color(148, 163, 184));
            g2.drawString(String.valueOf(labelVal), chartLeft - 24, y + 4);
            g2.setColor(new Color(241, 245, 249));
        }

        // Baseline axis
        g2.setColor(new Color(203, 213, 225));
        g2.drawLine(chartLeft, chartBottom, chartRight, chartBottom);

        // 3. Draw Bars
        ApplicationStage[] stages = ApplicationStage.values();
        int numBars = stages.length;
        int slotWidth = chartWidth / numBars;
        int barWidth = Math.min(60, slotWidth - 20);

        for (int i = 0; i < numBars; i++) {
            ApplicationStage stage = stages[i];
            int count = stageCounts.getOrDefault(stage, 0);
            int barHeight = (int) (((double) count / maxVal) * chartHeight);

            int x = chartLeft + (i * slotWidth) + (slotWidth - barWidth) / 2;
            int y = chartBottom - barHeight;

            Color baseColor = STAGE_COLORS.getOrDefault(stage, new Color(71, 85, 105));

            // Draw bar with gradient
            GradientPaint gradient = new GradientPaint(x, y, baseColor, x + barWidth, chartBottom, baseColor.darker());
            g2.setPaint(gradient);
            g2.fillRoundRect(x, y, barWidth, Math.max(barHeight, 3), 6, 6);

            // Draw count label on top of bar
            g2.setColor(new Color(30, 41, 59));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            String countText = String.valueOf(count);
            int countW = g2.getFontMetrics().stringWidth(countText);
            g2.drawString(countText, x + (barWidth - countW) / 2, y - 6);

            // Draw percentage label if > 0
            if (totalApplications > 0 && count > 0) {
                int pct = (int) Math.round((count * 100.0) / totalApplications);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(new Color(100, 116, 139));
                String pctText = pct + "%";
                int pctW = g2.getFontMetrics().stringWidth(pctText);
                g2.drawString(pctText, x + (barWidth - pctW) / 2, y - 20);
            }

            // Draw stage label below axis
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.setColor(new Color(71, 85, 105));
            String stageLabel = formatStageLabel(stage);
            int labelW = g2.getFontMetrics().stringWidth(stageLabel);
            g2.drawString(stageLabel, x + (barWidth - labelW) / 2, chartBottom + 16);

            // Subtitle status (e.g. "Active", "Final")
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(new Color(148, 163, 184));
            String sub = stage.isTerminal() ? "Terminal" : "In Flight";
            int subW = g2.getFontMetrics().stringWidth(sub);
            g2.drawString(sub, x + (barWidth - subW) / 2, chartBottom + 28);
        }

        g2.dispose();
    }

    private String formatStageLabel(ApplicationStage stage) {
        return switch (stage) {
            case DRAFT -> "Draft";
            case UNDER_REVIEW -> "Review";
            case CREDIT_APPROVAL -> "Approved";
            case DISBURSEMENT -> "Disbursed";
            case CORE_BANKING_PUSH -> "Pushing";
            case EXIT -> "Core Exit";
            case REJECTED -> "Rejected";
        };
    }
}
