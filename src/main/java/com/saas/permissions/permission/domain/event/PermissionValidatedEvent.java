package com.saas.permissions.permission.domain.event;

import java.time.OffsetDateTime;

public record PermissionValidatedEvent(
        String role,
        String route,
        boolean granted,
        String reason,
        double durationMs,
        OffsetDateTime occurredAt) {

}
