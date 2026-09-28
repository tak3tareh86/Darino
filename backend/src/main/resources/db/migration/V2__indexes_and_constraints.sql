-- V2__indexes_and_constraints.sql: Performance Indexes and Constraints for High Scalability

-- Users Indexes
CREATE INDEX idx_users_phone ON users(phone_number);
CREATE INDEX idx_users_email ON users(email);

-- Phone Verifications
CREATE INDEX idx_phone_verifications_phone_status ON phone_verifications(phone_number, status);
CREATE INDEX idx_phone_verifications_expires_at ON phone_verifications(expires_at);

-- Reminders & Schedules Indexes
CREATE INDEX idx_reminders_user_status ON reminders(user_id, status);
CREATE INDEX idx_reminders_due_at ON reminders(due_at);
CREATE INDEX idx_reminders_type ON reminders(type);

CREATE INDEX idx_reminder_schedules_reminder ON reminder_schedules(reminder_id);
CREATE INDEX idx_reminder_schedules_scheduled_status ON reminder_schedules(scheduled_at, status);

-- Notifications Indexes
CREATE INDEX idx_notifications_user_status ON notifications(user_id, status);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- SMS Jobs & Logs Indexes
CREATE INDEX idx_sms_jobs_status_retry ON sms_jobs(status, next_retry_at) WHERE status IN ('QUEUED', 'PROCESSING');
CREATE INDEX idx_sms_jobs_provider_msg ON sms_jobs(provider_message_id);
CREATE INDEX idx_sms_jobs_user ON sms_jobs(user_id);
CREATE INDEX idx_sms_logs_user ON sms_logs(user_id);
CREATE INDEX idx_sms_logs_provider_msg ON sms_logs(provider_message_id);

-- Installments & Vehicles
CREATE INDEX idx_installments_user ON installments(user_id);
CREATE INDEX idx_installments_next_due ON installments(next_due_date);
CREATE INDEX idx_vehicles_user ON vehicles(user_id);
