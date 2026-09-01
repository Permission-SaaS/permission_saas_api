CREATE TABLE role_routes (
    id            UUID         CONSTRAINT pk_role_routes PRIMARY KEY DEFAULT gen_random_uuid(),

    role_id       UUID         NOT NULL,
    route_id      UUID         NOT NULL,

    granted_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    revoked_at    TIMESTAMPTZ,

    CONSTRAINT fk_role_routes_role  FOREIGN KEY (role_id)  REFERENCES roles(id)  ON DELETE CASCADE,
    CONSTRAINT fk_role_routes_route FOREIGN KEY (route_id) REFERENCES routes(id) ON DELETE CASCADE,

    CONSTRAINT role_routes_revoked_after_granted CHECK (revoked_at IS NULL OR revoked_at >= granted_at)
);

CREATE UNIQUE INDEX uq_role_routes_active ON role_routes (role_id, route_id) WHERE revoked_at IS NULL;

CREATE INDEX idx_role_routes_role_id ON role_routes(role_id);
CREATE INDEX idx_role_routes_route_id ON role_routes(route_id);
CREATE INDEX idx_role_routes_granted_at ON role_routes(granted_at DESC);
