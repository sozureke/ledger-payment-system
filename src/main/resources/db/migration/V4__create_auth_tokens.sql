CREATE TABLE auth_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id),
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT auth_tokens_hash_uk    UNIQUE (token_hash),
    CONSTRAINT auth_tokens_expiry_chk CHECK (expires_at > created_at)
);

CREATE INDEX auth_tokens_user_id_idx ON auth_tokens (user_id);
