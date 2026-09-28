-- =====================================================================
-- Migration V1: Database Initial Schema for Finance & Reminder Platform
-- =====================================================================

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) UNIQUE,
    phone_number VARCHAR(20) UNIQUE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150),
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Refresh Tokens Table (with secure token rotation)
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Phone OTP Verifications Table
CREATE TABLE IF NOT EXISTS phone_verifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    phone_number VARCHAR(20) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, VERIFIED, EXPIRED, LOCKED
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resend_available_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    ip_address VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. Vehicles Table
CREATE TABLE IF NOT EXISTS vehicles (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    model_year VARCHAR(10),
    plate_number VARCHAR(50),
    odometer_km BIGINT DEFAULT 0,
    color VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Installments Table
CREATE TABLE IF NOT EXISTS installments (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    provider_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    total_amount NUMERIC(18, 2) NOT NULL,
    monthly_payment NUMERIC(18, 2) NOT NULL,
    total_installments INT NOT NULL,
    paid_installments INT NOT NULL DEFAULT 0,
    due_day INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Reminders Table (Central Reminder Entity)
CREATE TABLE IF NOT EXISTS reminders (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL, -- GENERAL, INSTALLMENT, VEHICLE, INSURANCE, MAINTENANCE, FUEL
    title VARCHAR(200) NOT NULL,
    description TEXT,
    due_at TIMESTAMP WITH TIME ZONE NOT NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Tehran',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, URGENT
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, COMPLETED, SNOOZED, CANCELLED
    notification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    phone_number VARCHAR(20),
    source_type VARCHAR(50), -- INSTALLMENT, VEHICLE, GENERAL
    source_id VARCHAR(36),
    repeat_rule VARCHAR(50) NOT NULL DEFAULT 'NONE',
    completed_at TIMESTAMP WITH TIME ZONE,
    snoozed_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. Reminder Schedules Table (Normalized Multi-Trigger Scheduling)
CREATE TABLE IF NOT EXISTS reminder_schedules (
    id VARCHAR(36) PRIMARY KEY,
    reminder_id VARCHAR(36) NOT NULL REFERENCES reminders(id) ON DELETE CASCADE,
    trigger_type VARCHAR(50) NOT NULL, -- BEFORE_DUE, EXACT_TIME, AFTER_DUE
    offset_value INT NOT NULL DEFAULT 0,
    offset_unit VARCHAR(20) NOT NULL DEFAULT 'DAYS', -- MINUTES, HOURS, DAYS, WEEKS, MONTHS
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    executed BOOLEAN NOT NULL DEFAULT FALSE,
    executed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. Notifications Table (In-App Notification Center)
CREATE TABLE IF NOT EXISTS notifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reminder_id VARCHAR(36) REFERENCES reminders(id) ON DELETE SET NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'UNREAD', -- UNREAD, READ, ARCHIVED
    action_url VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP WITH TIME ZONE
);

-- 9. Notification Logs Table
CREATE TABLE IF NOT EXISTS notification_logs (
    id VARCHAR(36) PRIMARY KEY,
    notification_id VARCHAR(36) REFERENCES notifications(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL,
    channel VARCHAR(30) NOT NULL, -- PUSH, IN_APP, EMAIL
    delivery_status VARCHAR(30) NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. SMS Jobs Table (Asynchronous Reliable SMS Queue)
CREATE TABLE IF NOT EXISTS sms_jobs (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reminder_id VARCHAR(36) REFERENCES reminders(id) ON DELETE SET NULL,
    phone_number VARCHAR(20) NOT NULL,
    template_id VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED', -- QUEUED, PROCESSING, SUBMITTED, SENT, DELIVERED, FAILED, EXPIRED
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 4,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    provider_name VARCHAR(50),
    provider_message_id VARCHAR(100),
    failure_reason TEXT,
    queued_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP WITH TIME ZONE,
    sent_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 11. SMS Logs Table
CREATE TABLE IF NOT EXISTS sms_logs (
    id VARCHAR(36) PRIMARY KEY,
    sms_job_id VARCHAR(36) REFERENCES sms_jobs(id) ON DELETE SET NULL,
    user_id VARCHAR(36) NOT NULL,
    reminder_id VARCHAR(36),
    phone_number_masked VARCHAR(30) NOT NULL,
    template_id VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_message_id VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 12. Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36),
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id VARCHAR(36),
    ip_address VARCHAR(45),
    user_agent VARCHAR(255),
    details JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
