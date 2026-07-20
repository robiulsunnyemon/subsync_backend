-- V1__init.sql
-- Baseline migration for SubSync database.
-- Note: As we are using baseline-on-migrate, this script will be marked as applied 
-- on existing databases that were created via Hibernate ddl-auto=update.

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    provider VARCHAR(50),
    provider_id VARCHAR(255),
    is_email_verified BOOLEAN DEFAULT FALSE,
    otp VARCHAR(255),
    otp_expiry TIMESTAMP,
    reset_password_token VARCHAR(255),
    reset_password_token_expiry TIMESTAMP,
    profile_image VARCHAR(255),
    bio TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    merchant_name VARCHAR(255) NOT NULL,
    amount DECIMAL NOT NULL,
    currency VARCHAR(10) NOT NULL,
    cycle VARCHAR(50) NOT NULL,
    next_billing_date DATE NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
