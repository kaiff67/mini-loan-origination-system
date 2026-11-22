package com.lending.los.model;

import java.time.LocalDateTime;

/**
 * ApplicationHistory entity representing audit trails of all workflow transitions.
 */
public class ApplicationHistory {
    private Long historyId;
    private Long applicationId;
    private String action;
    private String oldStatus;
    private String newStatus;
    private String performedBy;
    private LocalDateTime performedAt;
    private String remarks;

    public ApplicationHistory() {}

    public ApplicationHistory(Long applicationId, String action, String oldStatus, String newStatus,
                              String performedBy, LocalDateTime performedAt, String remarks) {
        this.applicationId = applicationId;
        this.action = action;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.performedBy = performedBy;
        this.performedAt = performedAt;
        this.remarks = remarks;
    }

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public LocalDateTime getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(LocalDateTime performedAt) {
        this.performedAt = performedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
