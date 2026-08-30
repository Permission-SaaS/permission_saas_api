package com.saas.permissions.audit.api.mapper;

import org.springframework.stereotype.Component;

import com.saas.permissions.audit.api.dto.AuditEventResponse;
import com.saas.permissions.audit.domain.AuditEvent;
import com.saas.permissions.shared.domain.Mapper;

@Component
public class AuditEventResponseMapper implements Mapper<AuditEvent, AuditEventResponse> {

    @Override
    public AuditEventResponse map(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.type(),
                event.getProjectId(),
                event.getOccurredAt() != null ? event.getOccurredAt().toString() : null,
                event.describe());
    }
}
