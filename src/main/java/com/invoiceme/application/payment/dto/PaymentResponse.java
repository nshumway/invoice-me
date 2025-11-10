package com.invoiceme.application.payment.dto;

import com.invoiceme.domain.payment.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Payment details response")
public class PaymentResponse {

    // === BaseEntity fields ===

    @Schema(description = "Payment ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Timestamp when payment was created", example = "2025-11-09T10:30:00Z")
    private Instant createdAt;

    @Schema(description = "User ID who created this payment", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID createdBy;

    @Schema(description = "Timestamp when payment was last modified", example = "2025-11-09T10:30:00Z")
    private Instant lastModifiedAt;

    @Schema(description = "User ID who last modified this payment", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID lastModifiedBy;

    @Schema(description = "Version number for optimistic locking", example = "1")
    private Long version;

    // === Payment fields ===

    @Schema(description = "ID of the invoice this payment is for", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID invoiceId;

    @Schema(description = "Date and time when the payment was received", example = "2025-11-09T10:30:00Z")
    private Instant paymentDate;

    @Schema(description = "Payment amount", example = "150.00")
    private BigDecimal amount;

    @Schema(description = "Payment method used", example = "CASH", allowableValues = {"CASH", "CHECK", "CREDIT_CARD", "BANK_TRANSFER", "OTHER"})
    private PaymentMethod paymentMethod;

    @Schema(description = "Reference number such as check number or transaction ID", example = "CHK-12345")
    private String referenceNumber;

    @Schema(description = "Additional notes about this payment", example = "Partial payment, remainder due next month")
    private String notes;

    // === Read-only fields ===

    @Schema(description = "Customer company name (denormalized)", example = "Acme Corporation")
    private String customerName;

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

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Instant getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Instant paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
}
