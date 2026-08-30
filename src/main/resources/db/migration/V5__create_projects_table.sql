CREATE TABLE projects (
    id            UUID         CONSTRAINT pk_projects PRIMARY KEY DEFAULT gen_random_uuid(),

    client_id     UUID         NOT NULL,

    name          VARCHAR(120) NOT NULL,
    description   VARCHAR(500),
    max_roles     INTEGER,

    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ,
    deleted_at    TIMESTAMPTZ,

    CONSTRAINT projects_max_roles_check CHECK (max_roles IS NULL OR max_roles > 0)
);

CREATE INDEX idx_projects_client_id ON projects(client_id);
CREATE INDEX idx_projects_active ON projects(created_at) WHERE deleted_at IS NULL;
