CREATE TABLE roles (
    id            UUID         CONSTRAINT pk_roles PRIMARY KEY DEFAULT gen_random_uuid(),

    project_id    UUID         NOT NULL,

    name          VARCHAR(80)  NOT NULL,
    description   VARCHAR(255),

    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ,

    CONSTRAINT fk_roles_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

CREATE INDEX idx_roles_project_id ON roles(project_id);
CREATE UNIQUE INDEX uq_roles_project_name ON roles (project_id, LOWER(name));
