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
