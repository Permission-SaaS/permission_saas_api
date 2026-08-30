CREATE TABLE routes (
    id            UUID         CONSTRAINT pk_routes PRIMARY KEY DEFAULT gen_random_uuid(),

    project_id    UUID         NOT NULL,

    name          VARCHAR(80)  NOT NULL,
    http_method   VARCHAR(10)  NOT NULL,
    path          VARCHAR(255) NOT NULL,
    description   VARCHAR(255),

    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ,

    CONSTRAINT fk_routes_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT routes_http_method_check CHECK (UPPER(http_method) IN ('GET', 'POST', 'PUT', 'PATCH', 'DELETE')),
    CONSTRAINT routes_path_check CHECK (path LIKE '/%')
);

CREATE INDEX idx_routes_project_id ON routes(project_id);
CREATE UNIQUE INDEX uq_routes_project_method_path ON routes (project_id, UPPER(http_method), LOWER(path));
