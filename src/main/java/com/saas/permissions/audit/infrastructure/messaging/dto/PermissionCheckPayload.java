package com.saas.permissions.audit.infrastructure.messaging.dto;

import java.util.UUID;

public record PermissionCheckPayload(
        UUID projectId,
        String routePath,
        String httpMethod,
        String roleName,
        boolean granted,
        String reason,
        double durationMs) {
}
