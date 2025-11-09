-- Create invoices table (Phase 4)
CREATE TABLE invoices (
    -- Primary key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Foreign key to customers
    customer_id UUID NOT NULL REFERENCES customers(id),

    -- User-editable fields
    invoice_number VARCHAR(50) NOT NULL,
    notes TEXT,

    -- Read-only fields (system-managed)
    invoice_date TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    customer_name VARCHAR(255) NOT NULL,
    total DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    amount_paid DECIMAL(19, 2) NOT NULL DEFAULT 0.00,

    -- Audit fields (from BaseEntity)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID NOT NULL,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_by UUID NOT NULL,

    -- Soft delete fields
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID,

    -- Optimistic locking
    version BIGINT NOT NULL DEFAULT 0
);

-- Unique invoice number for non-deleted invoices
CREATE UNIQUE INDEX idx_invoices_invoice_number_active
    ON invoices(invoice_number)
    WHERE is_deleted = FALSE;

-- Filter by status
CREATE INDEX idx_invoices_status
    ON invoices(status)
    WHERE is_deleted = FALSE;

-- Filter by customer
CREATE INDEX idx_invoices_customer_id
    ON invoices(customer_id)
    WHERE is_deleted = FALSE;

-- Sort by invoice date
CREATE INDEX idx_invoices_invoice_date
    ON invoices(invoice_date)
    WHERE is_deleted = FALSE;
