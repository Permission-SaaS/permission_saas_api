CREATE TABLE subscriptions (
    -- Primary Key
    id            UUID        CONSTRAINT pk_subscriptions PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Relationships
    client_id     UUID        NOT NULL,
    plan_id       UUID        NOT NULL,

    -- Status & Period
    status        VARCHAR(50) NOT NULL DEFAULT 'pending',
    starts_at     TIMESTAMPTZ,
    expires_at    TIMESTAMPTZ,

    -- Timestamps
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT fk_subscriptions_client FOREIGN KEY (client_id) REFERENCES clients(id),
    CONSTRAINT fk_subscriptions_plan FOREIGN KEY (plan_id) REFERENCES plans(id),
    CONSTRAINT subscriptions_status_check CHECK (status IN ('pending', 'active', 'canceled', 'expired'))
);

CREATE INDEX idx_subscriptions_client_id ON subscriptions(client_id);
CREATE INDEX idx_subscriptions_plan_id ON subscriptions(plan_id);

-- Garante que um Client não tenha duas Subscription 'active' simultâneas
CREATE UNIQUE INDEX uq_subscriptions_client_active
    ON subscriptions (client_id)
    WHERE status = 'active';
