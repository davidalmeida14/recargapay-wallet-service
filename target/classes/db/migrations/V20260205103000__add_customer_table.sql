--liquibase formatted sql
--changeset david:V20260205103000__add_customer_table

CREATE TABLE customers
(
    id         UUID PRIMARY KEY,
    full_name  VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_customers_email UNIQUE (email)
);

CREATE INDEX IF NOT EXISTS idx_customers_email ON customers (email);

ALTER TABLE wallets ADD CONSTRAINT fk_wallets_customer FOREIGN KEY (customer_id) REFERENCES customers(id);
