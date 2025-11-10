package com.invoiceme.application.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request to update an existing invoice")
public class UpdateInvoiceRequest {

    @NotNull(message = "Invoice ID is required")
    @Schema(description = "ID of the invoice to update", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
    private UUID id;

    @NotNull(message = "Version is required for optimistic locking")
    @Schema(description = "Current version of the invoice (for optimistic locking)", example = "0", required = true)
    private Long version;

    @Schema(description = "Invoice number", example = "INV-2025-11-09-001")
    private String invoiceNumber;

    @Schema(description = "Invoice notes or payment terms", example = "Payment terms: Net 30 days")
    private String notes;

    // Note: customerId and customerName are NOT included as they are immutable

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
