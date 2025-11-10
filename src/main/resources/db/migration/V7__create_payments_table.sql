-- Create payments table for tracking invoice payments

CREATE TABLE payments (
    -- Primary key
    id UUID PRIMARY KEY,

    -- Payment fields
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    payment_date TIMESTAMP NOT NULL,
    amount DECIMAL(19, 2) NOT NULL CHECK (amount > 0),
    payment_method VARCHAR(50) NOT NULL,
    reference_number VARCHAR(255),
    notes TEXT,

    -- Read-only fields (system managed)
    customer_name VARCHAR(255) NOT NULL,

    -- BaseEntity fields (audit and soft delete)
    created_at TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    last_modified_by UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

-- Index for filtering by invoice (most common query pattern)
CREATE INDEX idx_payments_invoice_id
    ON payments(invoice_id)
    WHERE is_deleted = FALSE;

-- Index for filtering by payment date (for reporting)
CREATE INDEX idx_payments_payment_date
    ON payments(payment_date DESC)
    WHERE is_deleted = FALSE;
