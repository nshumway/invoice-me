package com.invoiceme.domain.payment.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a payment is recorded.
 * This allows InvoiceService to recalculate amountPaid and check for status transition to PAID.
 */
public class PaymentRecordedEvent {

    private final UUID paymentId;
    private final UUID invoiceId;
    private final BigDecimal amount;
    private final Instant occurredAt;

    public PaymentRecordedEvent(UUID paymentId, UUID invoiceId, BigDecimal amount) {
        this.paymentId = paymentId;
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.occurredAt = Instant.now();
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "PaymentRecordedEvent{" +
                "paymentId=" + paymentId +
                ", invoiceId=" + invoiceId +
                ", amount=" + amount +
                ", occurredAt=" + occurredAt +
                '}';
    }
}
