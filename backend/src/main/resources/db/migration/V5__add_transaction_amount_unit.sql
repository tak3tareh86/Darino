-- V5: Persist the monetary unit alongside transaction amounts.
-- Existing transaction values were stored and exchanged as Toman by legacy clients.
ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS amount_unit VARCHAR(16) NOT NULL DEFAULT 'TOMAN';

ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_amount_unit
    CHECK (amount_unit IN ('TOMAN', 'RIAL'));
