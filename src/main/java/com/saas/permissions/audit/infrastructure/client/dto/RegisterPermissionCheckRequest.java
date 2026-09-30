package com.saas.permissions.audit.infrastructure.client.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegisterPermissionCheckRequest(
    UUID projectId,

    OffsetDateTime occurredAt,

    String routePath,

    String httpMethod,

    String roleName,

    Boolean granted,

    String reason,

    Double durationMs) {
}
