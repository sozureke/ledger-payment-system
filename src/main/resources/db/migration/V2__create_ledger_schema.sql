CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    kind          VARCHAR(10)  NOT NULL,
    name          VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT users_kind_chk     CHECK (kind IN ('CUSTOMER', 'SYSTEM')),
    CONSTRAINT users_password_chk CHECK ((kind = 'SYSTEM') = (password_hash IS NULL))
);

CREATE TABLE accounts (
    id         UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID           NOT NULL REFERENCES users (id),
    currency   VARCHAR(3)     NOT NULL,
    balance    NUMERIC(19, 4) NOT NULL DEFAULT 0,
    status     VARCHAR(10)    NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT accounts_user_currency_uk UNIQUE (user_id, currency),
    CONSTRAINT accounts_currency_chk     CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT accounts_status_chk       CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED'))
);

CREATE TABLE transactions (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    type       VARCHAR(20) NOT NULL,
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT transactions_type_chk   CHECK (type IN ('TRANSFER', 'TOPUP')),
    CONSTRAINT transactions_status_chk CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'))
);

CREATE TABLE entries (
    id             BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    transaction_id UUID           NOT NULL REFERENCES transactions (id),
    account_id     UUID           NOT NULL REFERENCES accounts (id),
    amount         NUMERIC(19, 4) NOT NULL,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT entries_amount_chk CHECK (amount <> 0)
);

CREATE INDEX entries_account_id_idx     ON entries (account_id, id);
CREATE INDEX entries_transaction_id_idx ON entries (transaction_id);