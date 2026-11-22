package com.lending.los.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Customer entity representing demographic and KYC records.
 */
public class Customer {
    private Long customerId;
    private String name;
    private LocalDate dateOfBirth;
    private String mobile;
    private String address;
    private String kycDetails;
    private LocalDateTime createdAt;

    public Customer() {}

    public Customer(String name, LocalDate dateOfBirth, String mobile, String address, String kycDetails) {
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.mobile = mobile;
        this.address = address;
        this.kycDetails = kycDetails;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getKycDetails() {
        return kycDetails;
    }

    public void setKycDetails(String kycDetails) {
        this.kycDetails = kycDetails;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + customerId +
                ", name='" + name + '\'' +
                ", dob=" + dateOfBirth +
                ", mobile='" + mobile + '\'' +
                '}';
    }
}
