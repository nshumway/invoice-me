package com.invoiceme.application.customer.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lightweight DTO for customer lists - only includes essential fields
 */
public class CustomerListItemResponse {

    private UUID id;
    private String companyName;
    private String email;
    private BigDecimal totalOutstanding;

    // === Getters and Setters ===

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getTotalOutstanding() {
        return totalOutstanding;
    }

    public void setTotalOutstanding(BigDecimal totalOutstanding) {
        this.totalOutstanding = totalOutstanding;
    }
}
