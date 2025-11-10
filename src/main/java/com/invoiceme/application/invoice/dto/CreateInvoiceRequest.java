package com.invoiceme.application.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request to create a new invoice")
public class CreateInvoiceRequest {

    @NotNull(message = "Customer ID is required")
    @Schema(description = "ID of the customer for this invoice", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
    private UUID customerId;

    @Schema(description = "Invoice number (optional - will be auto-generated if not provided)", example = "INV-2025-11-09-001")
    private String invoiceNumber;

    @Schema(description = "Invoice notes or payment terms", example = "Payment terms: Net 30 days")
    private String notes;

    // === Getters and Setters ===

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
}
