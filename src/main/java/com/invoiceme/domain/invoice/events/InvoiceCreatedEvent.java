package com.invoiceme.domain.invoice.events;

import com.invoiceme.domain.invoice.InvoiceStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when an invoice is created.
 * This allows other domains (e.g., Customer) to react to the creation without direct coupling.
 */
public class InvoiceCreatedEvent {

    private final UUID invoiceId;
    private final UUID customerId;
    private final InvoiceStatus status;
    private final Instant occurredAt;

    public InvoiceCreatedEvent(UUID invoiceId, UUID customerId, InvoiceStatus status) {
        this.invoiceId = invoiceId;
        this.customerId = customerId;
        this.status = status;
        this.occurredAt = Instant.now();
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "InvoiceCreatedEvent{" +
                "invoiceId=" + invoiceId +
                ", customerId=" + customerId +
                ", status=" + status +
                ", occurredAt=" + occurredAt +
                '}';
    }
}
