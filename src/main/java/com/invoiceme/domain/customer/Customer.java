package com.invoiceme.domain.customer;

import com.invoiceme.domain.common.BaseEntity;
import com.invoiceme.domain.common.exceptions.ValidationException;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    // === Fields ===

    @Column(nullable = false)
    private String companyName;

    private String contactFirstName;
    private String contactLastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    // === Read-Only Fields (System Managed) ===

    @Column(nullable = false)
    private Integer draftInvoiceCount = 0;

    @Column(nullable = false)
    private Integer sentInvoiceCount = 0;

    @Column(nullable = false)
    private Integer paidInvoiceCount = 0;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalOutstanding = BigDecimal.ZERO;

    // === Constructors ===

    public Customer() {
    }

    // === CREATE OPERATION ===

    /**
     * Validates business rules for customer creation.
     * Infrastructure-level validation (email uniqueness) should be done in service layer.
     */
    public void validateForCreate(String companyName, String email) {
        if (companyName == null || companyName.isBlank()) {
            throw new ValidationException("Company name is required");
        }
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required");
        }
    }

    /**
     * Creates a new customer with the provided data.
     * Call validateForCreate() before this method.
     */
    public void create(String companyName, String contactFirstName, String contactLastName,
                      String email, String phone, String addressLine1, String addressLine2,
                      String city, String state, String zipCode, String country) {
        this.companyName = companyName;
        this.contactFirstName = contactFirstName;
        this.contactLastName = contactLastName;
        this.email = email;
        this.phone = phone;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;

        // Initialize read-only fields
        this.draftInvoiceCount = 0;
        this.sentInvoiceCount = 0;
        this.paidInvoiceCount = 0;
        this.totalOutstanding = BigDecimal.ZERO;
    }

    // === UPDATE OPERATION ===

    /**
     * Validates business rules for customer update.
     * Infrastructure-level validation (email uniqueness) should be done in service layer.
     */
    public void validateForUpdate(String companyName, String email, boolean isSystemUpdate) {
        if (companyName == null || companyName.isBlank()) {
            throw new ValidationException("Company name is required");
        }
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required");
        }
    }

    /**
     * Updates customer fields.
     * Call validateForUpdate() before this method.
     */
    public void update(String companyName, String contactFirstName, String contactLastName,
                      String email, String phone, String addressLine1, String addressLine2,
                      String city, String state, String zipCode, String country) {
        this.companyName = companyName;
        this.contactFirstName = contactFirstName;
        this.contactLastName = contactLastName;
        this.email = email;
        this.phone = phone;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;
    }

    /**
     * Returns the old company name before update for cascade detection.
     * Service layer will check if name changed and cascade to invoices.
     */
    public String getOldCompanyName() {
        return this.companyName;
    }

    // === DELETE OPERATION ===

    /**
     * Performs soft delete of the customer.
     * Service layer should validate that customer can be deleted (no SENT invoices).
     */
    public void delete() {
        this.markAsDeleted(); // Soft delete from BaseEntity
    }

    // === Helper Methods for Statistics (Address Issue #4) ===

    /**
     * These methods are kept for backward compatibility but will be replaced
     * with database-level atomic operations to fix race conditions.
     * @deprecated Use CustomerService.updateStatistics() with atomic DB updates instead
     */
    @Deprecated
    public void incrementDraftInvoiceCount() {
        this.draftInvoiceCount++;
    }

    @Deprecated
    public void decrementDraftInvoiceCount() {
        this.draftInvoiceCount--;
    }

    @Deprecated
    public void incrementSentInvoiceCount() {
        this.sentInvoiceCount++;
    }

    @Deprecated
    public void decrementSentInvoiceCount() {
        this.sentInvoiceCount--;
    }

    @Deprecated
    public void incrementPaidInvoiceCount() {
        this.paidInvoiceCount++;
    }

    @Deprecated
    public void decrementPaidInvoiceCount() {
        this.paidInvoiceCount--;
    }

    @Deprecated
    public void addToTotalOutstanding(BigDecimal amount) {
        this.totalOutstanding = this.totalOutstanding.add(amount);
    }

    @Deprecated
    public void subtractFromTotalOutstanding(BigDecimal amount) {
        this.totalOutstanding = this.totalOutstanding.subtract(amount);
    }

    // === Getters and Setters ===

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
