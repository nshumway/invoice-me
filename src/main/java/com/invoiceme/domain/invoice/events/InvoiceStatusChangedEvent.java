package com.invoiceme.domain.invoice.events;

import com.invoiceme.domain.invoice.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when an invoice's status changes.
 * This allows other domains (e.g., Customer) to react to status transitions without direct coupling.
 */
public class InvoiceStatusChangedEvent {

    private final UUID invoiceId;
    private final UUID customerId;
    private final InvoiceStatus oldStatus;
    private final InvoiceStatus newStatus;
    private final BigDecimal invoiceTotal;
    private final Instant occurredAt;

    public InvoiceStatusChangedEvent(UUID invoiceId, UUID customerId,
                                   InvoiceStatus oldStatus, InvoiceStatus newStatus,
                                   BigDecimal invoiceTotal) {
        this.invoiceId = invoiceId;
        this.customerId = customerId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.invoiceTotal = invoiceTotal;
        this.occurredAt = Instant.now();
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public InvoiceStatus getOldStatus() {
        return oldStatus;
    }

    public InvoiceStatus getNewStatus() {
        return newStatus;
    }

    public BigDecimal getInvoiceTotal() {
        return invoiceTotal;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "InvoiceStatusChangedEvent{" +
                "invoiceId=" + invoiceId +
                ", customerId=" + customerId +
                ", oldStatus=" + oldStatus +
                ", newStatus=" + newStatus +
                ", invoiceTotal=" + invoiceTotal +
                ", occurredAt=" + occurredAt +
                '}';
    }
}
