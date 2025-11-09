-- Create customers table
CREATE TABLE customers (
    -- Primary key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Customer fields
    company_name VARCHAR(255) NOT NULL,
    contact_first_name VARCHAR(255),
    contact_last_name VARCHAR(255),
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    zip_code VARCHAR(20),
    country VARCHAR(100),

    -- Read-only system-managed fields
    draft_invoice_count INTEGER NOT NULL DEFAULT 0,
    sent_invoice_count INTEGER NOT NULL DEFAULT 0,
    paid_invoice_count INTEGER NOT NULL DEFAULT 0,
    total_outstanding DECIMAL(19, 2) NOT NULL DEFAULT 0.00,

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

-- Create index on email for faster lookups
CREATE INDEX idx_customers_email ON customers(email) WHERE is_deleted = FALSE;

-- Create index on company_name for sorting and searching
CREATE INDEX idx_customers_company_name ON customers(company_name) WHERE is_deleted = FALSE;

-- Create index for soft delete queries
CREATE INDEX idx_customers_is_deleted ON customers(is_deleted);
