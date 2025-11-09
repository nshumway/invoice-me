package com.invoiceme.application.invoice.dto;

import java.util.UUID;

/**
 * Mock UpdateInvoiceRequest - will be fully implemented in Phase 3
 * This is a placeholder to satisfy Customer entity dependencies
 */
public class UpdateInvoiceRequest {
    private UUID id;
    private Long version;
    private String customerName;

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

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
}
