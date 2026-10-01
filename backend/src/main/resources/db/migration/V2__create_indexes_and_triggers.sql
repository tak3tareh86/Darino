-- =====================================================================
-- Migration V2: Unified Indexes, Constraints, and Performance Optimizations
-- =====================================================================

-- Users
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone_number);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Refresh Tokens
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_hash ON refresh_tokens(token_hash);

-- Phone OTP & Verifications
CREATE INDEX IF NOT EXISTS idx_phone_verifications_phone ON phone_verifications(phone_number);
CREATE INDEX IF NOT EXISTS idx_phone_verifications_status ON phone_verifications(status);
CREATE INDEX IF NOT EXISTS idx_phone_verifications_phone_status ON phone_verifications(phone_number, status);
CREATE INDEX IF NOT EXISTS idx_phone_verifications_expires_at ON phone_verifications(expires_at);

-- Reminders
CREATE INDEX IF NOT EXISTS idx_reminders_user ON reminders(user_id);
CREATE INDEX IF NOT EXISTS idx_reminders_user_status ON reminders(user_id, status);
CREATE INDEX IF NOT EXISTS idx_reminders_status ON reminders(status);
CREATE INDEX IF NOT EXISTS idx_reminders_due_at ON reminders(due_at);
CREATE INDEX IF NOT EXISTS idx_reminders_type ON reminders(type);
CREATE INDEX IF NOT EXISTS idx_reminders_source ON reminders(source_type, source_id);

-- Reminder Schedules
CREATE INDEX IF NOT EXISTS idx_reminder_schedules_reminder ON reminder_schedules(reminder_id);
CREATE INDEX IF NOT EXISTS idx_reminder_schedules_scheduled ON reminder_schedules(scheduled_at, enabled, executed);

-- Notifications
CREATE INDEX IF NOT EXISTS idx_notifications_user_status ON notifications(user_id, status);
CREATE INDEX IF NOT EXISTS idx_notifications_created ON notifications(created_at DESC);

-- SMS Jobs
CREATE INDEX IF NOT EXISTS idx_sms_jobs_queue ON sms_jobs(status, next_retry_at, queued_at);
CREATE INDEX IF NOT EXISTS idx_sms_jobs_status_retry ON sms_jobs(status, next_retry_at);
CREATE INDEX IF NOT EXISTS idx_sms_jobs_provider_msg ON sms_jobs(provider_message_id);
CREATE INDEX IF NOT EXISTS idx_sms_jobs_idempotency ON sms_jobs(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_sms_jobs_user ON sms_jobs(user_id);

-- SMS Logs
CREATE INDEX IF NOT EXISTS idx_sms_logs_user ON sms_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_sms_logs_provider_msg ON sms_logs(provider_message_id);

-- Vehicles & Installments
CREATE INDEX IF NOT EXISTS idx_vehicles_user ON vehicles(user_id);

-- Audit Logs
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_created ON audit_logs(user_id, created_at DESC);
