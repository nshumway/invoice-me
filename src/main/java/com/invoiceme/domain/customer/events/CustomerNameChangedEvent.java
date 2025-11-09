package com.invoiceme.domain.customer.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a customer's company name is changed.
 * This allows other domains (e.g., Invoice) to react to the change without direct coupling.
 */
public class CustomerNameChangedEvent {

    private final UUID customerId;
    private final String oldCompanyName;
    private final String newCompanyName;
    private final Instant occurredAt;

    public CustomerNameChangedEvent(UUID customerId, String oldCompanyName, String newCompanyName) {
        this.customerId = customerId;
        this.oldCompanyName = oldCompanyName;
        this.newCompanyName = newCompanyName;
        this.occurredAt = Instant.now();
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public String getOldCompanyName() {
        return oldCompanyName;
    }

    public String getNewCompanyName() {
        return newCompanyName;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public String toString() {
        return "CustomerNameChangedEvent{" +
                "customerId=" + customerId +
                ", oldCompanyName='" + oldCompanyName + '\'' +
                ", newCompanyName='" + newCompanyName + '\'' +
                ", occurredAt=" + occurredAt +
                '}';
    }
}
