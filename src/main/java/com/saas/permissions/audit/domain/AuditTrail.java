package com.saas.permissions.audit.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditTrail {
    void record(PermissionCheckEvent event);

    List<AuditTrailEntry> search(UUID projectId, String type, Boolean onlyDenied, OffsetDateTime from,
            OffsetDateTime to);
}
