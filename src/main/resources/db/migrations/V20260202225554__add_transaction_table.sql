--liquibase formatted sql
--changeset david:V20260202225554__add_transaction_table


CREATE TABLE IF NOT EXISTS transactions
(
    id                    UUID PRIMARY KEY,
    wallet_id             UUID NOT NULL,
    wallet_destination_id UUID,
    idempotency_id        VARCHAR(255),
    amount                DECIMAL(19, 2),
    type                  VARCHAR(50),
    status                VARCHAR(50),
    created_at            TIMESTAMP WITH TIME ZONE,
    updated_at            TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_wallet
        FOREIGN KEY (wallet_id)
            REFERENCES wallets (id),
    CONSTRAINT unkn_wallet_destination_id
        UNIQUE (wallet_id, idempotency_id, type)
);

CREATE INDEX IF NOT EXISTS idx_transaction_wallet_id ON transactions (wallet_id);
CREATE INDEX IF NOT EXISTS idx_transaction_wallet_id_created_at ON transactions (wallet_id, created_at);
CREATE INDEX IF NOT EXISTS idx_transaction_wallet_type ON transactions (wallet_id, type) INCLUDE (created_at);
--rollback
