package com.invoiceme.application.payment.dto;

import com.invoiceme.domain.payment.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Request to record a payment for an invoice")
public class RecordPaymentRequest {

    @NotNull(message = "Invoice ID is required")
    @Schema(description = "ID of the invoice this payment is for", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
    private UUID invoiceId;

    @NotNull(message = "Payment date is required")
    @Schema(description = "Date and time when the payment was received (ISO 8601 format)", example = "2025-11-09T10:30:00Z", required = true)
    private Instant paymentDate;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Schema(description = "Payment amount", example = "150.00", required = true)
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    @Schema(description = "Payment method used", example = "CASH", required = true, allowableValues = {"CASH", "CHECK", "CREDIT_CARD", "BANK_TRANSFER", "OTHER"})
    private PaymentMethod paymentMethod;

    @Schema(description = "Reference number such as check number or transaction ID", example = "CHK-12345")
    private String referenceNumber;

    @Schema(description = "Additional notes about this payment", example = "Partial payment, remainder due next month")
    private String notes;

    // === Getters and Setters ===

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
}
