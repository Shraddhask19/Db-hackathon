-- 4. Accounts Table
CREATE TABLE accounts (
    account_id UUID DEFAULT gen_random_uuid(),
    account_number VARCHAR(20) NOT NULL,
    customer_id UUID NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    available_balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT pk_accounts PRIMARY KEY (account_id),
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT,
    CONSTRAINT uq_accounts_number UNIQUE (account_number),
    CONSTRAINT chk_accounts_balances CHECK (available_balance <= balance),
    CONSTRAINT chk_accounts_type CHECK (account_type IN ('SAVINGS', 'CHECKING', 'LOAN', 'CREDIT')),
    CONSTRAINT chk_accounts_status CHECK (status IN ('ACTIVE', 'FROZEN', 'DORMANT', 'CLOSED'))
);

CREATE INDEX idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX idx_accounts_status ON accounts(status);

-- 5. Cards Table
CREATE TABLE cards (
    card_id UUID DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    card_number_hash VARCHAR(64) NOT NULL,
    card_last_four VARCHAR(4) NOT NULL,
    card_type VARCHAR(10) NOT NULL,
    expiry_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    daily_limit NUMERIC(15, 2) NOT NULL DEFAULT 1000.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT pk_cards PRIMARY KEY (card_id),
    CONSTRAINT fk_cards_account FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE RESTRICT,
    CONSTRAINT uq_cards_number_hash UNIQUE (card_number_hash),
    CONSTRAINT chk_cards_type CHECK (card_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_cards_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED', 'EXPIRED'))
);

CREATE INDEX idx_cards_account ON cards(account_id);
CREATE INDEX idx_cards_status ON cards(status);

-- 6. Beneficiaries Table
CREATE TABLE beneficiaries (
    beneficiary_id UUID DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    beneficiary_name VARCHAR(100) NOT NULL,
    account_number VARCHAR(34) NOT NULL,
    bank_routing_code VARCHAR(20) NOT NULL,
    nickname VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT pk_beneficiaries PRIMARY KEY (beneficiary_id),
    CONSTRAINT fk_beneficiaries_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

CREATE INDEX idx_beneficiaries_customer ON beneficiaries(customer_id);
