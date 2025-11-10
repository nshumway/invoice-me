-- V6__create_line_items_table.sql
-- Creates the line_items table for storing invoice line items

CREATE TABLE line_items (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    description TEXT NOT NULL,
    quantity DECIMAL(19, 4) NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL,

    -- Read-only fields
    customer_name VARCHAR(255) NOT NULL,
    line_total DECIMAL(19, 2) NOT NULL,

    -- BaseEntity fields
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
CREATE INDEX idx_line_items_invoice_id
    ON line_items(invoice_id)
    WHERE is_deleted = FALSE;

-- Index for audit queries
CREATE INDEX idx_line_items_created_at
    ON line_items(created_at);
