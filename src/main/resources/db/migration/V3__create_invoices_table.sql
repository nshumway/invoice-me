-- Create invoices table (mock/placeholder for Phase 3)
CREATE TABLE invoices (
    -- Primary key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Invoice fields (minimal for now)
    customer_id UUID NOT NULL,
    customer_name VARCHAR(255),
    status VARCHAR(50) NOT NULL,

    -- Audit fields (from BaseEntity)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by UUID,

    -- Soft delete fields
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID,

    -- Optimistic locking
    version BIGINT NOT NULL DEFAULT 0
);

-- Create index on customer_id for customer invoice lookups
CREATE INDEX idx_invoices_customer_id ON invoices(customer_id) WHERE is_deleted = FALSE;

-- Create index on status for filtering
CREATE INDEX idx_invoices_status ON invoices(status) WHERE is_deleted = FALSE;

-- Create index for soft delete queries
CREATE INDEX idx_invoices_is_deleted ON invoices(is_deleted);
