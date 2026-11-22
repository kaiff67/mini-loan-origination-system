package com.lending.los.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Service to manage configurable business dates.
 * As required by business specification:
 * "Use INR and a configurable business date, defaulting to today.
 *  Business timestamps use that date with the current time."
 */
public class BusinessDateService {

    private static LocalDate currentBusinessDate = LocalDate.now();

    public static synchronized LocalDate getBusinessDate() {
        return currentBusinessDate;
    }

    public static synchronized void setBusinessDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Business date cannot be null.");
        }
        currentBusinessDate = date;
    }

    public static synchronized void advanceDays(long days) {
        currentBusinessDate = currentBusinessDate.plusDays(days);
    }

    /**
     * Returns a timestamp using the configured business date coupled with current local time.
     */
    public static synchronized LocalDateTime getBusinessDateTime() {
        return LocalDateTime.of(currentBusinessDate, LocalTime.now());
    }

    public static synchronized String getFormattedBusinessDate() {
        return currentBusinessDate.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
    }
}
