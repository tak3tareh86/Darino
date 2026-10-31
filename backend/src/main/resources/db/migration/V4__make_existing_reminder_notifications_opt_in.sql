-- Legacy reminders were created while notification_enabled defaulted to TRUE.
-- Require users to explicitly opt existing reminders back into push delivery.
UPDATE reminders SET notification_enabled = FALSE WHERE notification_enabled = TRUE;

ALTER TABLE reminders
    ALTER COLUMN notification_enabled SET DEFAULT FALSE;
