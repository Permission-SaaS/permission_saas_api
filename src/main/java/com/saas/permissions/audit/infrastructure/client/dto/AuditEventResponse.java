package com.saas.permissions.audit.infrastructure.client.dto;

import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        String type,
        UUID projectId,
        String occurredAt,
        String description) {

}
