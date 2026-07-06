CREATE TABLE clients (
    -- Primary Key
    id              UUID        CONSTRAINT pk_clients PRIMARY KEY DEFAULT gen_random_uuid(),

    -- User Information
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL CONSTRAINT uq_clients_email UNIQUE,
    phone           VARCHAR(20),
    password_hash   TEXT,
    
    -- Authentication Provider
    provider        VARCHAR(100),
    provider_id     VARCHAR(255),
    
    -- Status & Account Management
    status          VARCHAR(50)  NOT NULL DEFAULT 'active',
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    blocked         BOOLEAN      NOT NULL DEFAULT FALSE,
    login_attempts  INTEGER      NOT NULL DEFAULT 0,
    
    -- Timestamps
    block_expires_at    TIMESTAMPTZ,
    email_verified_at   TIMESTAMPTZ,
    last_login_at       TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    
    -- Constraints
    CONSTRAINT clients_status_check CHECK (status IN ('active', 'inactive', 'blocked', 'deleted', 'pending')),
    CONSTRAINT clients_provider_check CHECK (provider IN ('local', 'google', 'facebook', 'github'))
);
