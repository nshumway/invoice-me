package com.invoiceme.domain.invoice.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when an invoice's customer name changes (due to customer name update).
 * This allows LineItemService to update denormalized customerName field on all line items
 * for the affected invoice.
 */
public class InvoiceCustomerNameChangedEvent {

    private final UUID invoiceId;
    private final String newCustomerName;
    private final Instant occurredAt;

    public InvoiceCustomerNameChangedEvent(UUID invoiceId, String newCustomerName) {
        this.invoiceId = invoiceId;
        this.newCustomerName = newCustomerName;
        this.occurredAt = Instant.now();
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public String getNewCustomerName() {
        return newCustomerName;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "InvoiceCustomerNameChangedEvent{" +
                "invoiceId=" + invoiceId +
                ", newCustomerName='" + newCustomerName + '\'' +
                ", occurredAt=" + occurredAt +
                '}';
    }
}
