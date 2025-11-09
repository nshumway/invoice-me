package com.invoiceme.application.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public class UpdateCustomerRequest {

    @NotNull(message = "Customer ID is required")
    private UUID id;

    @NotNull(message = "Version is required for optimistic locking")
    private Long version;

    @NotBlank(message = "Company name is required")
    private String companyName;

    private String contactFirstName;
    private String contactLastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    // === Read-only fields (only for system updates) ===
    private Integer draftInvoiceCount;
    private Integer sentInvoiceCount;
    private Integer paidInvoiceCount;
    private BigDecimal totalOutstanding;

    // === Getters and Setters ===

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactFirstName() {
        return contactFirstName;
    }

    public void setContactFirstName(String contactFirstName) {
        this.contactFirstName = contactFirstName;
    }

    public String getContactLastName() {
        return contactLastName;
    }

    public void setContactLastName(String contactLastName) {
        this.contactLastName = contactLastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getDraftInvoiceCount() {
        return draftInvoiceCount;
    }

    public void setDraftInvoiceCount(Integer draftInvoiceCount) {
        this.draftInvoiceCount = draftInvoiceCount;
    }

    public Integer getSentInvoiceCount() {
        return sentInvoiceCount;
    }

    public void setSentInvoiceCount(Integer sentInvoiceCount) {
        this.sentInvoiceCount = sentInvoiceCount;
    }

    public Integer getPaidInvoiceCount() {
        return paidInvoiceCount;
    }

    public void setPaidInvoiceCount(Integer paidInvoiceCount) {
        this.paidInvoiceCount = paidInvoiceCount;
    }

    public BigDecimal getTotalOutstanding() {
        return totalOutstanding;
    }

    public void setTotalOutstanding(BigDecimal totalOutstanding) {
        this.totalOutstanding = totalOutstanding;
    }
}
