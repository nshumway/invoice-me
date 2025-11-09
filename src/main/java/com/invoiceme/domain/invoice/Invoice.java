package com.invoiceme.domain.invoice;

import com.invoiceme.domain.common.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

/**
 * Mock Invoice entity - will be fully implemented in Phase 3
 * This is a placeholder to satisfy Customer entity dependencies
 */
@Entity
@Table(name = "invoices")
public class Invoice extends BaseEntity {

    @Column(nullable = false)
    private UUID customerId;

    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    // === Getters and Setters ===

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }
}
