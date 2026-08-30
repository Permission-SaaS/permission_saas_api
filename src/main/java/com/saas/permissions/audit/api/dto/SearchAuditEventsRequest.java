package com.saas.permissions.audit.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.Pattern;

public record SearchAuditEventsRequest(

        @Pattern(regexp = "(?i)PERMISSION_CHECK|PROJECT_LIFECYCLE", message = "must be a known audit event type") String type,

        UUID projectId,

        Boolean onlyDenied) {

}
