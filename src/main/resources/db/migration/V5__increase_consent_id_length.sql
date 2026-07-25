-- V5__increase_consent_id_length.sql
ALTER TABLE bank_connections ALTER COLUMN consent_id TYPE VARCHAR(2048);
