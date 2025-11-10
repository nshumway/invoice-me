-- Update invoices table for Phase 4 implementation
-- This migration transforms the placeholder invoice table into the full schema

-- First, drop the old indexes
DROP INDEX IF EXISTS idx_invoices_customer_id;
DROP INDEX IF EXISTS idx_invoices_status;
DROP INDEX IF EXISTS idx_invoices_is_deleted;

-- Delete any existing placeholder data (this is safe since V3 was just a placeholder)
DELETE FROM invoices;

-- Modify the table structure
ALTER TABLE invoices
    -- Add foreign key constraint
    ADD CONSTRAINT fk_invoices_customer_id FOREIGN KEY (customer_id) REFERENCES customers(id),

    -- Add new columns
    ADD COLUMN invoice_number VARCHAR(50) NOT NULL,
    ADD COLUMN notes TEXT,
    ADD COLUMN invoice_date TIMESTAMP,
    ADD COLUMN total DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    ADD COLUMN amount_paid DECIMAL(19, 2) NOT NULL DEFAULT 0.00,

    -- Modify existing columns
    ALTER COLUMN customer_name SET NOT NULL,
    ALTER COLUMN status SET DEFAULT 'DRAFT',
    ALTER COLUMN status TYPE VARCHAR(20),
    ALTER COLUMN created_by SET NOT NULL,
    ALTER COLUMN last_modified_by SET NOT NULL;

-- Create new indexes
CREATE UNIQUE INDEX idx_invoices_invoice_number_active
    ON invoices(invoice_number)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_invoices_status
    ON invoices(status)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_invoices_customer_id
    ON invoices(customer_id)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_invoices_invoice_date
    ON invoices(invoice_date)
    WHERE is_deleted = FALSE;
