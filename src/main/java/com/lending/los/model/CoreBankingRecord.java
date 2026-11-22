package com.lending.los.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * CoreBankingRecord represents entries pushed to the NG_CORE_BANKING external table.
 */
public class CoreBankingRecord {
    private Long coreId;
    private Long customerId;
    private Long applicationId;
    private String customerName;
    private String address;
    private LocalDate dob;
    private String kycDetails;
    private BigDecimal approvedAmount;
    private BigDecimal disbursedAmount;
    private LocalDateTime disbursedDate;

    public CoreBankingRecord() {}

    public CoreBankingRecord(Long customerId, Long applicationId, String customerName,
                             String address, LocalDate dob, String kycDetails,
                             BigDecimal approvedAmount, BigDecimal disbursedAmount,
                             LocalDateTime disbursedDate) {
        this.customerId = customerId;
        this.applicationId = applicationId;
        this.customerName = customerName;
        this.address = address;
        this.dob = dob;
        this.kycDetails = kycDetails;
        this.approvedAmount = approvedAmount;
        this.disbursedAmount = disbursedAmount;
        this.disbursedDate = disbursedDate;
    }

    public Long getCoreId() {
        return coreId;
    }

    public void setCoreId(Long coreId) {
        this.coreId = coreId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getDob() {
        return dob;
    }

    public void setDob(LocalDate dob) {
        this.dob = dob;
    }

    public String getKycDetails() {
        return kycDetails;
    }

    public void setKycDetails(String kycDetails) {
        this.kycDetails = kycDetails;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public BigDecimal getDisbursedAmount() {
        return disbursedAmount;
    }

    public void setDisbursedAmount(BigDecimal disbursedAmount) {
        this.disbursedAmount = disbursedAmount;
    }

    public LocalDateTime getDisbursedDate() {
        return disbursedDate;
    }

    public void setDisbursedDate(LocalDateTime disbursedDate) {
        this.disbursedDate = disbursedDate;
    }
}
