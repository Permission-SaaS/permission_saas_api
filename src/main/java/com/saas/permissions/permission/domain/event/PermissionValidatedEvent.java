package com.saas.permissions.permission.domain.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PermissionValidatedEvent(
        UUID projectId,
        String role,
        String httpMethod,
        String route,
        boolean granted,
        String reason,
        double durationMs,
        OffsetDateTime occurredAt) {

}
