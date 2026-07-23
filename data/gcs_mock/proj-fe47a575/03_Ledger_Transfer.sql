-- 7. Transactions Table (Partitioned by Month)
CREATE TABLE transactions (
    transaction_id BIGINT GENERATED ALWAYS AS IDENTITY,
    account_id UUID NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reference_number VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT pk_transactions PRIMARY KEY (transaction_id, created_at),
    CONSTRAINT uq_transactions_reference UNIQUE (reference_number, created_at),
    CONSTRAINT chk_transactions_amount CHECK (amount > 0),
    CONSTRAINT chk_transactions_type CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT', 'FEE', 'INTEREST')),
    CONSTRAINT chk_transactions_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED'))
) PARTITION BY RANGE (created_at);

-- Example Partition
CREATE TABLE transactions_y2026m07 PARTITION OF transactions
    FOR VALUES FROM ('2026-07-01 00:00:00+00') TO ('2026-08-01 00:00:00+00');

CREATE INDEX idx_transactions_account_created ON transactions(account_id, created_at DESC);
CREATE INDEX idx_transactions_status ON transactions(status) WHERE status = 'PENDING';

-- 8. Transfers Table
CREATE TABLE transfers (
    transfer_id UUID DEFAULT gen_random_uuid(),
    source_account_id UUID NOT NULL,
    destination_account_id UUID NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    initiated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    
    -- Constraints
    CONSTRAINT pk_transfers PRIMARY KEY (transfer_id),
    CONSTRAINT fk_transfers_source_account FOREIGN KEY (source_account_id) REFERENCES accounts(account_id),
    CONSTRAINT fk_transfers_dest_account FOREIGN KEY (destination_account_id) REFERENCES accounts(account_id),
    CONSTRAINT chk_transfers_different_accounts CHECK (source_account_id <> destination_account_id),
    CONSTRAINT chk_transfers_amount CHECK (amount > 0),
    CONSTRAINT chk_transfers_status CHECK (status IN ('PROCESSING', 'SUCCESS', 'FAILED'))
);

CREATE INDEX idx_transfers_source ON transfers(source_account_id, initiated_at DESC);
CREATE INDEX idx_transfers_destination ON transfers(destination_account_id, initiated_at DESC);

-- 9. Account Holds Table
CREATE TABLE account_holds (
    hold_id UUID DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    reason VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMPTZ,
    
    -- Constraints
    CONSTRAINT pk_account_holds PRIMARY KEY (hold_id),
    CONSTRAINT fk_holds_account FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE RESTRICT,
    CONSTRAINT chk_holds_amount CHECK (amount > 0),
    CONSTRAINT chk_holds_status CHECK (status IN ('ACTIVE', 'RELEASED', 'SETTLED'))
);

CREATE INDEX idx_holds_account_status ON account_holds(account_id, status) WHERE status = 'ACTIVE';

-- 10. Account Balance History Table (Partitioned by Month)
CREATE TABLE account_balance_history (
    history_id BIGINT GENERATED ALWAYS AS IDENTITY,
    account_id UUID NOT NULL,
    snapshot_date DATE NOT NULL,
    closing_balance NUMERIC(15, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT pk_account_balance_history PRIMARY KEY (account_id, snapshot_date)
) PARTITION BY RANGE (snapshot_date);

-- Example Partition
CREATE TABLE account_balance_history_y2026m07 PARTITION OF account_balance_history
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');

CREATE INDEX idx_bal_hist_account_date ON account_balance_history(account_id, snapshot_date DESC);
