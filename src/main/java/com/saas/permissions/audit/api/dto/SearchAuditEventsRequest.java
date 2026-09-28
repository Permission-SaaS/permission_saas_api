package com.saas.permissions.audit.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;

public record SearchAuditEventsRequest(
                @Pattern(regexp = "(?i)PERMISSION_CHECK|PROJECT_LIFECYCLE", message = "must be a known audit event type") String type,

                UUID projectId,

                Boolean onlyDenied,

                @DateTimeFormat(iso = ISO.DATE_TIME) @PastOrPresent OffsetDateTime from,
                @DateTimeFormat(iso = ISO.DATE_TIME) OffsetDateTime to) {

}
