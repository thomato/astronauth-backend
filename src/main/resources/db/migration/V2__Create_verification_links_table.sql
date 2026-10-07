CREATE TABLE verification_links (
    token_hash                 CHAR(64) PRIMARY KEY,
    account_id                 UUID NOT NULL REFERENCES accounts (id),
    password_hash              VARCHAR(255) NOT NULL,
    sent_at                    TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at                    TIMESTAMP WITH TIME ZONE,
    invalidated_at             TIMESTAMP WITH TIME ZONE,
    wrong_credential_attempts  INT NOT NULL DEFAULT 0
);

CREATE INDEX verification_links_account_id ON verification_links (account_id);
