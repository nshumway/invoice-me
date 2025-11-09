package com.invoiceme.application.customer.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class DeleteCustomerRequest {

    @NotNull(message = "Customer ID is required")
    private UUID id;

    @NotNull(message = "Version is required for optimistic locking")
    private Long version;

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
}
