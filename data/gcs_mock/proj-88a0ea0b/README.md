# High-Throughput Core Banking OLTP System

A production-grade, enterprise-ready **PostgreSQL Database Schema & Architectural Specification** designed for core banking OLTP (Online Transaction Processing) workloads. 

This repository contains the complete DDL (Data Definition Language) scripts with explicit Foreign Keys (FK), Primary Keys (PK), Check Constraints, Unique Constraints, partitioning strategies, indexing models, data dictionary, and end-to-end domain lifecycle stories for a 15-table core banking database engine.

---

## 📑 Table of Contents
- [Architecture & Design Principles](#-architecture--design-principles)
- [System Entity Relationship Diagram](#-system-entity-relationship-diagram)
- [Explicit Constraints & Foreign Key Matrix](#-explicit-constraints--foreign-key-matrix)
- [Complete Schema DDL (15 Tables)](#-complete-schema-ddl-15-tables)
  - [Domain 1: Customer Profile & Security](#domain-1-customer-profile--security)
  - [Domain 2: Core Accounts & Payment Rails](#domain-2-core-accounts--payment-rails)
  - [Domain 3: High-Volume Ledger & Transfers](#domain-3-high-volume-ledger--transfers)
  - [Domain 4: Loans & Credit Infrastructure](#domain-4-loans--credit-infrastructure)
  - [Domain 5: Auditing, Alerts & Integrations](#domain-5-auditing-alerts--integrations)
- [End-to-End System Narrative](#-end-to-end-system-narrative)
- [Non-Functional & Operational Strategy](#-non-functional--operational-strategy)

---

## 🏗 Architecture & Design Principles

1. **Exact-Decimal Precision (`NUMERIC(15,2)`):** All monetary balances, transaction limits, and holds use `NUMERIC(15,2)` to guarantee absolute floating-point precision and compliance with accounting rules.
2. **Declarative Range Partitioning:** Rapidly expanding time-series logs (`transactions`, `audit_logs`, `notifications`, `account_balance_history`, `loan_repayment_schedules`) are range-partitioned by month/date. This maintains fast B-Tree index lookup speeds and enables zero-downtime data archival/purging.
3. **Explicit Referential Integrity:** All tables use explicit constraint syntax (`CONSTRAINT pk_*`, `CONSTRAINT fk_*`, `CONSTRAINT chk_*`, `CONSTRAINT uq_*`) for precise migration management, foreign key cascading safety, and transactional protection.
4. **High-Concurrency Indexing:** Features selective **partial indexes** (e.g., `WHERE status = 'PENDING'` or `WHERE status = 'ACTIVE'`) to keep index footprints compact while speeding up queue workers and state-filtering queries.

---

## 📐 System Entity Relationship Diagram

```
                              +-------------------------+
                              |   webhook_subscriptions |
                              +-------------------------+
                                           |
                                           | (1:N)
+------------------------+    (1:N)  +------------------+    (1:N)  +--------------------+
|  customer_identities   | <---------|    CUSTOMERS     | --------> |  customer_devices  |
+------------------------+           +------------------+           +--------------------+
                                           |        |
                                           | (1:N)  | (1:N)
                                           v        v
                                   +---------------+  +---------------+
                                   | beneficiaries |  | notifications |
                                   +---------------+  +---------------+
                                           |
                                           | (Linked via customer context)
                                           v
                                    +--------------+
                                    |   ACCOUNTS   |
                                    +--------------+
                                     /    |     \
                              (1:N) /     |      \ (1:N)
                                   /      |       \
                                  v       |        v
                       +--------------+   |   +--------------+
                       |    CARDS     |   |   |    LOANS     |
                       +--------------+   |   +--------------+
                                          |          |
                                    (1:N) |          | (1:1)
                                          v          v
                                 +------------------------+
                                 | account_holds /        |
                                 | account_balance_history|
                                 +------------------------+
                                          |
                                          | (1:N)
                                          v
                                 +------------------------+
                                 |      TRANSACTIONS      |
                                 |           &            |
                                 |       TRANSFERS        |
                                 +------------------------+
                                          |
                                          v
                                 +------------------------+
                                 |       AUDIT_LOGS       |
                                 +------------------------+
```

---

## 🔗 Explicit Constraints & Foreign Key Matrix

| Table Name | Primary Key (PK) | Foreign Keys (FK) & Target Tables | Unique Constraints | Check Constraints |
|---|---|---|---|---|
| `customers` | `customer_id` | *None* | `customer_number`, `email`, `phone_number` | `kyc_status`, `status` |
| `customer_identities` | `identity_id` | `customer_id` $ightarrow$ `customers(customer_id)` | (`id_type`, `id_number`) | `id_type` |
| `customer_devices` | `device_id` | `customer_id` $ightarrow$ `customers(customer_id)` | (`customer_id`, `device_fingerprint`) | *None* |
| `accounts` | `account_id` | `customer_id` $ightarrow$ `customers(customer_id)` | `account_number` | `account_type`, `status`, `available_balance <= balance` |
| `cards` | `card_id` | `account_id` $ightarrow$ `accounts(account_id)` | `card_number_hash` | `card_type`, `status` |
| `beneficiaries` | `beneficiary_id` | `customer_id` $ightarrow$ `customers(customer_id)` | *None* | *None* |
| `transactions` | (`transaction_id`, `created_at`) | *Ledger Partition Key* | `reference_number` | `transaction_type`, `amount > 0`, `status` |
| `transfers` | `transfer_id` | `source_account_id` $ightarrow$ `accounts`<br>`destination_account_id` $ightarrow$ `accounts` | *None* | `amount > 0`, `status`, `source <> destination` |
| `account_holds` | `hold_id` | `account_id` $ightarrow$ `accounts(account_id)` | *None* | `amount > 0`, `status` |
| `account_balance_history` | (`account_id`, `snapshot_date`) | *Ledger Partition Key* | *None* | *None* |
| `loans` | `loan_id` | `customer_id` $ightarrow$ `customers`<br>`account_id` $ightarrow$ `accounts` | `account_id` | `loan_type`, `principal > 0`, `rate >= 0`, `status` |
| `loan_repayment_schedules` | (`loan_id`, `due_date`) | *Partition Key* | *None* | `installment_number > 0`, `status` |
| `audit_logs` | (`log_id`, `created_at`) | *Log Partition Key* | *None* | *None* |
| `notifications` | (`notification_id`, `created_at`) | *Queue Partition Key* | *None* | `channel`, `status` |
| `webhook_subscriptions` | `subscription_id` | `customer_id` $ightarrow$ `customers(customer_id)` | *None* | *None* |

---

## 💾 Complete Schema DDL (15 Tables)

### Domain 1: Customer Profile & Security

```sql
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
```

---

### Domain 2: Core Accounts & Payment Rails

```sql
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
```

---

### Domain 3: High-Volume Ledger & Transfers

```sql
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
```

---

### Domain 4: Loans & Credit Infrastructure

```sql
-- 11. Loans Table
CREATE TABLE loans (
    loan_id UUID DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    account_id UUID NOT NULL,
    loan_type VARCHAR(30) NOT NULL,
    principal_amount NUMERIC(15, 2) NOT NULL,
    outstanding_balance NUMERIC(15, 2) NOT NULL,
    interest_rate NUMERIC(5, 4) NOT NULL,
    term_months INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPLIED',
    start_date DATE,
    end_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT pk_loans PRIMARY KEY (loan_id),
    CONSTRAINT fk_loans_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT,
    CONSTRAINT fk_loans_account FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE RESTRICT,
    CONSTRAINT uq_loans_account UNIQUE (account_id),
    CONSTRAINT chk_loans_principal CHECK (principal_amount > 0),
    CONSTRAINT chk_loans_outstanding CHECK (outstanding_balance >= 0),
    CONSTRAINT chk_loans_interest_rate CHECK (interest_rate >= 0),
    CONSTRAINT chk_loans_term CHECK (term_months > 0),
    CONSTRAINT chk_loans_type CHECK (loan_type IN ('PERSONAL', 'MORTGAGE', 'AUTO', 'BUSINESS')),
    CONSTRAINT chk_loans_status CHECK (status IN ('APPLIED', 'APPROVED', 'ACTIVE', 'DEFAULTED', 'PAID_OFF'))
);

CREATE INDEX idx_loans_customer ON loans(customer_id);
CREATE INDEX idx_loans_status ON loans(status) WHERE status = 'ACTIVE';

-- 12. Loan Repayment Schedules Table (Partitioned by Month)
CREATE TABLE loan_repayment_schedules (
    schedule_id BIGINT GENERATED ALWAYS AS IDENTITY,
    loan_id UUID NOT NULL,
    installment_number INT NOT NULL,
    due_date DATE NOT NULL,
    principal_component NUMERIC(15, 2) NOT NULL,
    interest_component NUMERIC(15, 2) NOT NULL,
    total_due NUMERIC(15, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMPTZ,
    
    -- Constraints
    CONSTRAINT pk_loan_repayment_schedules PRIMARY KEY (loan_id, due_date),
    CONSTRAINT chk_schedules_installment CHECK (installment_number > 0),
    CONSTRAINT chk_schedules_status CHECK (status IN ('PENDING', 'PAID', 'OVERDUE', 'PARTIALLY_PAID'))
) PARTITION BY RANGE (due_date);

-- Example Partition
CREATE TABLE loan_repayment_schedules_y2026m07 PARTITION OF loan_repayment_schedules
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');

CREATE INDEX idx_loan_schedule_overdue ON loan_repayment_schedules(due_date, status) 
    WHERE status IN ('PENDING', 'OVERDUE');
```

---

### Domain 5: Auditing, Alerts & Integrations

```sql
-- 13. Audit Logs Table (Partitioned by Month)
CREATE TABLE audit_logs (
    log_id BIGINT GENERATED ALWAYS AS IDENTITY,
    actor_id VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(30) NOT NULL,
    entity_id VARCHAR(50) NOT NULL,
    payload JSONB,
    ip_address INET,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT pk_audit_logs PRIMARY KEY (log_id, created_at)
) PARTITION BY RANGE (created_at);

-- Example Partition
CREATE TABLE audit_logs_y2026m07 PARTITION OF audit_logs
    FOR VALUES FROM ('2026-07-01 00:00:00+00') TO ('2026-08-01 00:00:00+00');

CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_actor ON audit_logs(actor_id, created_at DESC);

-- 14. Notifications Log Table (Partitioned by Month)
CREATE TABLE notifications (
    notification_id BIGINT GENERATED ALWAYS AS IDENTITY,
    customer_id UUID NOT NULL,
    channel VARCHAR(10) NOT NULL,
    recipient_address VARCHAR(100) NOT NULL,
    template_code VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT pk_notifications PRIMARY KEY (notification_id, created_at),
    CONSTRAINT chk_notifications_channel CHECK (channel IN ('SMS', 'EMAIL', 'PUSH')),
    CONSTRAINT chk_notifications_status CHECK (status IN ('QUEUED', 'SENT', 'FAILED'))
) PARTITION BY RANGE (created_at);

-- Example Partition
CREATE TABLE notifications_y2026m07 PARTITION OF notifications
    FOR VALUES FROM ('2026-07-01 00:00:00+00') TO ('2026-08-01 00:00:00+00');

CREATE INDEX idx_notifications_customer ON notifications(customer_id, created_at DESC);
CREATE INDEX idx_notifications_queue ON notifications(status) WHERE status = 'QUEUED';

-- 15. Webhook Subscriptions Table
CREATE TABLE webhook_subscriptions (
    subscription_id UUID DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    target_url TEXT NOT NULL,
    secret_key VARCHAR(64) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT pk_webhook_subscriptions PRIMARY KEY (subscription_id),
    CONSTRAINT fk_webhooks_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE
);

CREATE INDEX idx_webhooks_active ON webhook_subscriptions(event_type) WHERE is_active = TRUE;
CREATE INDEX idx_webhooks_customer ON webhook_subscriptions(customer_id);
```

---

## 📖 End-to-End System Narrative

### 1. Onboarding & Security Verification
* A customer signs up $\rightarrow$ A master record is created in `CUSTOMERS`.
* The customer uploads a passport $\rightarrow$ Saved in `CUSTOMER_IDENTITIES` (`id_type = 'PASSPORT'`). Once verified, `CUSTOMERS.kyc_status` updates from `PENDING` to `VERIFIED`.
* First mobile app login $\rightarrow$ The device fingerprint and push token are stored in `CUSTOMER_DEVICES`.

### 2. Account & Card Provisioning
* A checking account is created in `ACCOUNTS` with `$0.00` balance.
* A debit card is provisioned in `CARDS` linked to `account_id` (storing the hashed card payload in `card_number_hash`).
* The user registers a beneficiary in `BENEFICIARIES` for quick transfers.

### 3. POS Transaction & Funds Transfer
* **Card Swipe Hold:** The user purchases an item for $5.00 $\rightarrow$ An active entry is added to `ACCOUNT_HOLDS`. The `available_balance` on `ACCOUNTS` drops by $5.00 while maintaining the primary ledger balance.
* **Peer-to-Peer Transfer:** User sends $500.00 to a friend $\rightarrow$
  * `TRANSFERS` creates a parent record connecting source and destination accounts.
  * `TRANSACTIONS` creates two partitioned entries: `TRANSFER_OUT` (source) and `TRANSFER_IN` (destination).
  * An event is placed in `NOTIFICATIONS` (`channel = 'PUSH'`) to alert the user device.
  * An outbound webhook payload is dispatched via `WEBHOOK_SUBSCRIPTIONS`.
  * An immutable record is created in `AUDIT_LOGS`.
  * End-of-day balances are summarized into `ACCOUNT_BALANCE_HISTORY`.

### 4. Loan Disbursal & Installment Repayment
* Customer applies for a $10,000 auto loan $\rightarrow$ Recorded in `LOANS`.
* An amortization schedule is generated and bulk-inserted into `LOAN_REPAYMENT_SCHEDULES`.
* Auto-debits process monthly installments, updating `LOAN_REPAYMENT_SCHEDULES.status` to `PAID`.

---

## ⚡ Non-Functional & Operational Strategy

* **Isolation Level:** Set transaction isolation level to `REPEATABLE READ` or `SERIALIZABLE` for account-to-account transfer transactions to eliminate race conditions and lost updates.
* **Concurrency Locking:** Explicit `SELECT ... FOR UPDATE` row locks are recommended when updating account balances in `ACCOUNTS`.
* **Partition Maintenance:** Integrate `pg_partman` extension to automatically create upcoming monthly partitions and drop or detach old historical partitions.
* **Data Security & Privacy:** Sensitivity fields (e.g., card hashes, identity numbers) should utilize `pgcrypto` or application-level encryption at rest.
