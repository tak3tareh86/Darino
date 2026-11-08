CREATE TABLE IF NOT EXISTS user_subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    product_id VARCHAR(150) NOT NULL,
    purchase_token TEXT NOT NULL,
    initiation_timestamp_msec BIGINT NOT NULL,
    valid_until_timestamp_msec BIGINT NOT NULL,
    auto_renewing BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_subscriptions_valid_until
    ON user_subscriptions(valid_until_timestamp_msec);
