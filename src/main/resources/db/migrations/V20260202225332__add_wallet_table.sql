--liquibase formatted sql
--changeset david:V20260202225332__add_wallet_table

CREATE TABLE wallets
(
    id          UUID PRIMARY KEY,
    customer_id UUID       NOT NULL,
    balance     DECIMAL(19, 2)    NOT NULL,
    currency    VARCHAR(3) NOT NULL,
    active      BOOLEAN    NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP,
    CONSTRAINT unq_customer_currency UNIQUE (customer_id, currency)
);

CREATE INDEX IF NOT EXISTS idx_wallets_customer_id ON wallets (customer_id);

--rollback
