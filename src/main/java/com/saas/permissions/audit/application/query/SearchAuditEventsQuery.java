package com.saas.permissions.audit.application.query;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SearchAuditEventsQuery(
        String type,
        UUID projectId,
        Boolean onlyDenied,
        OffsetDateTime from,
        OffsetDateTime to) {

}
