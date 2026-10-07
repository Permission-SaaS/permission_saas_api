package com.saas.permissions.audit.domain;

import java.util.UUID;

public record AuditTrailEntry(
        UUID id,
        String type,
        UUID projectId,
        String occurredAt,
        String description) {

}
