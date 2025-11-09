package com.invoiceme.application.common;

import java.util.UUID;

/**
 * ThreadLocal storage for current user ID throughout request lifecycle.
 * Used for audit trail (createdBy, lastModifiedBy) even during cascading updates.
 */
public class UserContext {

    private static final ThreadLocal<UUID> currentUserId = new ThreadLocal<>();

    public static void setCurrentUser(UUID userId) {
        currentUserId.set(userId);
    }

    public static UUID getCurrentUser() {
        UUID userId = currentUserId.get();
        if (userId == null) {
            throw new IllegalStateException("No user context available");
        }
        return userId;
    }

    public static void clear() {
        currentUserId.remove();
    }
}
