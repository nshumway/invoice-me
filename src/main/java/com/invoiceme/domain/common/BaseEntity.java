package com.invoiceme.domain.common;

import com.invoiceme.application.common.UserContext;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private Instant lastModifiedAt;

    @Column(nullable = false)
    private UUID lastModifiedBy;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    private Instant deletedAt;

    private UUID deletedBy;

    // === Lifecycle Methods ===

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        UUID currentUser = UserContext.getCurrentUser();

        this.createdAt = now;
        this.createdBy = currentUser;
        this.lastModifiedAt = now;
        this.lastModifiedBy = currentUser;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastModifiedAt = Instant.now();
        this.lastModifiedBy = UserContext.getCurrentUser();
    }

    // === Soft Delete ===

    public void markAsDeleted() {
        this.isDeleted = true;
        this.deletedAt = Instant.now();
        this.deletedBy = UserContext.getCurrentUser();
    }

    // === Getters and Setters ===

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public UUID getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public UUID getDeletedBy() {
        return deletedBy;
    }
}
