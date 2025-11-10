package com.invoiceme.application.invoice.dto;

import com.invoiceme.domain.invoice.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Full invoice details response")
public class InvoiceResponse {

    // BaseEntity fields
    @Schema(description = "Invoice ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "When the invoice was created", example = "2025-11-09T10:30:00Z")
    private Instant createdAt;

    @Schema(description = "User who created the invoice", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID createdBy;

    @Schema(description = "When the invoice was last modified", example = "2025-11-09T14:30:00Z")
    private Instant lastModifiedAt;

    @Schema(description = "User who last modified the invoice", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID lastModifiedBy;

    @Schema(description = "Version for optimistic locking", example = "0")
    private Long version;

    // Invoice fields
    @Schema(description = "Customer ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID customerId;

    @Schema(description = "Invoice number", example = "INV-2025-11-09-001")
    private String invoiceNumber;

    @Schema(description = "Invoice notes or payment terms", example = "Payment terms: Net 30 days")
    private String notes;

    // Read-only fields
    @Schema(description = "Date when invoice was sent (null for DRAFT invoices)", example = "2025-11-09T14:30:00Z")
    private Instant invoiceDate;

    @Schema(description = "Invoice status", example = "DRAFT", allowableValues = {"DRAFT", "SENT", "PAID"})
    private InvoiceStatus status;

    @Schema(description = "Customer company name (denormalized)", example = "Acme Corp")
    private String customerName;

    @Schema(description = "Total amount of invoice", example = "1500.00")
    private BigDecimal total;

    @Schema(description = "Amount paid so far", example = "500.00")
    private BigDecimal amountPaid;

    // Calculated field (not stored in DB)
    @Schema(description = "Outstanding balance (total - amountPaid)", example = "1000.00")
    private BigDecimal balance;

    // === Getters and Setters ===

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(Instant lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public UUID getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(UUID lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(Instant invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
