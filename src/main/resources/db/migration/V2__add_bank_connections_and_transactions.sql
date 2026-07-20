-- V2__add_bank_connections_and_transactions.sql

CREATE TABLE IF NOT EXISTS bank_connections (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    provider VARCHAR(50) NOT NULL,
    institution_id VARCHAR(255) NOT NULL,
    institution_name VARCHAR(255) NOT NULL,
    account_id VARCHAR(255),
    consent_id VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY,
    bank_connection_id UUID NOT NULL REFERENCES bank_connections(id),
    external_transaction_id VARCHAR(255) NOT NULL,
    amount DECIMAL NOT NULL,
    currency VARCHAR(10) NOT NULL,
    date DATE NOT NULL,
    merchant_name VARCHAR(255),
    category VARCHAR(255),
    description TEXT
);
