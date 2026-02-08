--liquibase formatted sql
--changeset david:V20260202230050__add_entry_table

CREATE TABLE IF NOT EXISTS entries
(
    id             UUID PRIMARY KEY,
    wallet_id      UUID           NOT NULL,
    transaction_id UUID           NOT NULL,
    amount         DECIMAL(19,2)        NOT NULL,
    financial_type VARCHAR(50) NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE    NOT NULL,
    FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    CONSTRAINT unk_wallet_transaction_financial_type_amount UNIQUE (wallet_id, transaction_id, financial_type, amount)
);

CREATE INDEX IF NOT EXISTS idx_entry_wallet_id ON entries (wallet_id);
CREATE INDEX IF NOT EXISTS idx_entry_transaction_id ON entries (transaction_id);
--rollback
