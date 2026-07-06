CREATE TABLE api_keys (
    -- Primary Key
    id                 UUID        CONSTRAINT pk_api_keys PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Relationships
    subscription_id    UUID        NOT NULL,

    -- Key Data
    key_hash           TEXT        NOT NULL,
    active             BOOLEAN     NOT NULL DEFAULT TRUE,

    -- Timestamps
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at         TIMESTAMPTZ,

    CONSTRAINT fk_api_keys_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id)
);

CREATE INDEX idx_api_keys_subscription_id ON api_keys(subscription_id);

-- Garante que uma Subscription não tenha duas ApiKey ativas simultâneas
-- (key_hash não serve pra isso: é gerado com salt/BCrypt, nunca colide)
CREATE UNIQUE INDEX uq_api_keys_subscription_active
    ON api_keys (subscription_id)
    WHERE active = TRUE;
