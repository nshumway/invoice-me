package com.invoiceme.domain.lineitem.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a line item is created, updated, or deleted.
 * This allows InvoiceService to recalculate the invoice total without direct coupling.
 */
public class LineItemChangedEvent {

    private final UUID invoiceId;
    private final UUID lineItemId;
    private final LineItemChangeType changeType;
    private final Instant occurredAt;

    public LineItemChangedEvent(UUID invoiceId, UUID lineItemId, LineItemChangeType changeType) {
        this.invoiceId = invoiceId;
        this.lineItemId = lineItemId;
        this.changeType = changeType;
        this.occurredAt = Instant.now();
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public UUID getLineItemId() {
        return lineItemId;
    }

    public LineItemChangeType getChangeType() {
        return changeType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "LineItemChangedEvent{" +
                "invoiceId=" + invoiceId +
                ", lineItemId=" + lineItemId +
                ", changeType=" + changeType +
                ", occurredAt=" + occurredAt +
                '}';
    }
}
