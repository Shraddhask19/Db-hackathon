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
