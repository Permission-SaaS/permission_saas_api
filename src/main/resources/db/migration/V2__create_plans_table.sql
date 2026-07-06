CREATE TABLE plans (
    -- Primary Key
    id                      UUID          CONSTRAINT pk_plans PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Plan Information
    name                    VARCHAR(255)  NOT NULL,
    description             TEXT,
    max_projects            INTEGER       NOT NULL,
    max_users_per_project   INTEGER       NOT NULL,
    price                   DECIMAL(10,2) NOT NULL,

    -- Status
    active                  BOOLEAN       NOT NULL DEFAULT TRUE,

    -- Timestamps
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_plans_name UNIQUE (name)
);
