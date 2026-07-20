-- V3__add_notification_settings.sql

CREATE TABLE IF NOT EXISTS notification_settings (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    push_enabled BOOLEAN NOT NULL,
    email_enabled BOOLEAN NOT NULL,
    sms_enabled BOOLEAN NOT NULL,
    reminder_days_before INT NOT NULL
);
