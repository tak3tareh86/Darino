-- V3: Canonical per-user transaction store with idempotent client identity and tombstones.
CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    client_id VARCHAR(100) NOT NULL,
    amount BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    category VARCHAR(150) NOT NULL,
    account_name VARCHAR(150) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    time_formatted VARCHAR(50) NOT NULL DEFAULT '',
    title VARCHAR(255) NOT NULL DEFAULT '',
    sub_category VARCHAR(150),
    date_persian VARCHAR(50) NOT NULL DEFAULT '',
    payment_method VARCHAR(50) NOT NULL DEFAULT 'BANK_CARD',
    source_type VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    source_id VARCHAR(100),
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_transactions_user_client UNIQUE (user_id, client_id)
);

CREATE INDEX IF NOT EXISTS idx_transactions_user_occurred
    ON transactions (user_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_user_updated
    ON transactions (user_id, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_transactions_user_deleted
    ON transactions (user_id, deleted_at);
