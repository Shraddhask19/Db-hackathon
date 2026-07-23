-- 1. Customers Table
CREATE TABLE customers (
    customer_id UUID DEFAULT gen_random_uuid(),
    customer_number VARCHAR(20) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    kyc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT pk_customers PRIMARY KEY (customer_id),
    CONSTRAINT uq_customers_number UNIQUE (customer_number),
    CONSTRAINT uq_customers_email UNIQUE (email),
    CONSTRAINT uq_customers_phone UNIQUE (phone_number),
    CONSTRAINT chk_customers_kyc_status CHECK (kyc_status IN ('PENDING', 'VERIFIED', 'REJECTED', 'EXPIRED')),
    CONSTRAINT chk_customers_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
);

CREATE INDEX idx_customers_phone ON customers(phone_number);
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_status ON customers(status);

-- 2. Customer Identities Table (KYC Documents)
CREATE TABLE customer_identities (
    identity_id UUID DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    id_type VARCHAR(30) NOT NULL,
    id_number VARCHAR(100) NOT NULL,
    expiry_date DATE,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT pk_customer_identities PRIMARY KEY (identity_id),
    CONSTRAINT fk_identities_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT,
    CONSTRAINT uq_identities_type_number UNIQUE (id_type, id_number),
    CONSTRAINT chk_identities_id_type CHECK (id_type IN ('PASSPORT', 'NATIONAL_ID', 'DRIVERS_LICENSE', 'TAX_ID'))
);

CREATE INDEX idx_identities_customer_id ON customer_identities(customer_id);

-- 3. Customer Devices / MFA Sessions Table
CREATE TABLE customer_devices (
    device_id UUID DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    device_fingerprint VARCHAR(128) NOT NULL,
    device_name VARCHAR(100),
    os_version VARCHAR(50),
    push_notification_token TEXT,
    is_trusted BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT pk_customer_devices PRIMARY KEY (device_id),
    CONSTRAINT fk_devices_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    CONSTRAINT uq_devices_customer_fingerprint UNIQUE (customer_id, device_fingerprint)
);

CREATE INDEX idx_devices_customer ON customer_devices(customer_id);
