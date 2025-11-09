package com.invoiceme.domain.invoice.events;

import com.invoiceme.domain.invoice.InvoiceStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when an invoice is deleted (soft delete).
 * This allows other domains (e.g., Customer) to react to the deletion without direct coupling.
 */
public class InvoiceDeletedEvent {

    private final UUID invoiceId;
    private final UUID customerId;
    private final InvoiceStatus invoiceStatus;
    private final Instant occurredAt;

    public InvoiceDeletedEvent(UUID invoiceId, UUID customerId, InvoiceStatus invoiceStatus) {
        this.invoiceId = invoiceId;
        this.customerId = customerId;
        this.invoiceStatus = invoiceStatus;
        this.occurredAt = Instant.now();
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public InvoiceStatus getInvoiceStatus() {
        return invoiceStatus;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "InvoiceDeletedEvent{" +
                "invoiceId=" + invoiceId +
                ", customerId=" + customerId +
                ", invoiceStatus=" + invoiceStatus +
                ", occurredAt=" + occurredAt +
                '}';
    }
}
