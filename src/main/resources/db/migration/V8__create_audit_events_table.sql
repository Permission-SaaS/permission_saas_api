CREATE TABLE audit_events (
    id            UUID         CONSTRAINT pk_audit_events PRIMARY KEY DEFAULT gen_random_uuid(),

    event_type    VARCHAR(30)  NOT NULL,

    project_id    UUID,
    occurred_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    route_path    VARCHAR(255),
    http_method   VARCHAR(10),
    role_name     VARCHAR(80),
    granted       BOOLEAN,
    reason        VARCHAR(255),
    duration_ms   DOUBLE PRECISION,
    ip_address    VARCHAR(45),
    country       VARCHAR(60),

    action        VARCHAR(20),
    project_name  VARCHAR(120),
    performed_by  UUID,

    CONSTRAINT audit_events_type_check CHECK (event_type IN ('PERMISSION_CHECK', 'PROJECT_LIFECYCLE')),
    CONSTRAINT audit_events_action_check CHECK (action IS NULL OR action IN ('CREATED', 'UPDATED', 'DELETED')),
    CONSTRAINT audit_events_permission_check_fields CHECK (
        event_type <> 'PERMISSION_CHECK'
        OR (route_path IS NOT NULL AND granted IS NOT NULL AND duration_ms IS NOT NULL)
    ),
    CONSTRAINT audit_events_lifecycle_fields CHECK (
        event_type <> 'PROJECT_LIFECYCLE'
        OR (action IS NOT NULL AND project_id IS NOT NULL)
    )
);

CREATE INDEX idx_audit_events_occurred_at ON audit_events(occurred_at DESC);
CREATE INDEX idx_audit_events_project_id ON audit_events(project_id);
CREATE INDEX idx_audit_events_type ON audit_events(event_type);
CREATE INDEX idx_audit_events_denied ON audit_events(occurred_at DESC) WHERE granted = FALSE;
