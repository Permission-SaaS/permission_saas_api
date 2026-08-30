package com.saas.permissions.audit.application.command;

import java.util.UUID;

public record AuditEventQuery(
        String type,
        UUID projectId,
        Boolean onlyDenied) {

}
