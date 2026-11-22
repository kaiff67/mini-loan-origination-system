package com.lending.los.model;

import java.time.LocalDateTime;

/**
 * SystemAlert entity representing idempotent alerts (e.g. REVIEW_OVERDUE).
 */
public class SystemAlert {
    private Long alertId;
    private Long applicationId;
    private String alertType;
    private LocalDateTime createdAt;

    public SystemAlert() {}

    public SystemAlert(Long applicationId, String alertType, LocalDateTime createdAt) {
        this.applicationId = applicationId;
        this.alertType = alertType;
        this.createdAt = createdAt;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
