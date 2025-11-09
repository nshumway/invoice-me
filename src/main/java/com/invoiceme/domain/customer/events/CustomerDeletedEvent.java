package com.invoiceme.domain.customer.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a customer is deleted (soft deleted).
 * This allows other domains to react to the deletion (e.g., cascade delete invoices).
 */
public class CustomerDeletedEvent {

    private final UUID customerId;
    private final String companyName;
    private final Instant occurredAt;

    public CustomerDeletedEvent(UUID customerId, String companyName) {
        this.customerId = customerId;
        this.companyName = companyName;
        this.occurredAt = Instant.now();
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "CustomerDeletedEvent{" +
                "customerId=" + customerId +
                ", companyName='" + companyName + '\'' +
                ", occurredAt=" + occurredAt +
                '}';
    }
}
