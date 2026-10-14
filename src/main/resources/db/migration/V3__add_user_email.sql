ALTER TABLE users ADD COLUMN email VARCHAR(255);

UPDATE users
SET email = id::text || '@placeholder.invalid'
WHERE kind = 'CUSTOMER' AND email IS NULL;

ALTER TABLE users
    ADD CONSTRAINT users_email_chk       CHECK ((kind = 'SYSTEM') = (email IS NULL)),
    ADD CONSTRAINT users_email_lower_chk CHECK (email = lower(btrim(email)));

CREATE UNIQUE INDEX users_email_uk ON users (email);
