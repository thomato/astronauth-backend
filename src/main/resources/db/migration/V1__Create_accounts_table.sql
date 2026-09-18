CREATE TABLE accounts (
    id                UUID PRIMARY KEY,
    email             VARCHAR(254) NOT NULL,
    canonical_email   VARCHAR(254) NOT NULL UNIQUE,
    password_hash     VARCHAR(255) NOT NULL,
    email_verified_at TIMESTAMP WITH TIME ZONE,
    registered_at     TIMESTAMP WITH TIME ZONE NOT NULL
);
