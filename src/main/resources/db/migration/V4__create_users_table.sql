CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,

    -- Audit fields
    created_at TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    last_modified_by UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    -- Soft delete
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

-- Unique email for non-deleted users
CREATE UNIQUE INDEX idx_users_email_active
    ON users(email)
    WHERE is_deleted = FALSE;

-- Index for authentication lookups
CREATE INDEX idx_users_email
    ON users(email)
    WHERE is_deleted = FALSE;
