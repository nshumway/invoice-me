package com.invoiceme.application.invoice.dto;

import com.invoiceme.domain.invoice.InvoiceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Summary of invoice for list views")
public class InvoiceListItemResponse {

    @Schema(description = "Invoice ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Invoice number", example = "INV-2025-11-09-001")
    private String invoiceNumber;

    @Schema(description = "Invoice status", example = "DRAFT", allowableValues = {"DRAFT", "SENT", "PAID"})
    private InvoiceStatus status;

    @Schema(description = "Customer company name", example = "Acme Corp")
    private String customerName;

    @Schema(description = "Total amount of invoice", example = "1500.00")
    private BigDecimal total;

    @Schema(description = "Amount paid so far", example = "500.00")
    private BigDecimal amountPaid;

    @Schema(description = "Outstanding balance (total - amountPaid)", example = "1000.00")
    private BigDecimal balance;

    // === Getters and Setters ===

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
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
