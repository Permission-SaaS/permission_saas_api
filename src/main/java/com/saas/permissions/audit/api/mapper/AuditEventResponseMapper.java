package com.saas.permissions.audit.api.mapper;

import org.springframework.stereotype.Component;

import com.saas.permissions.audit.api.dto.AuditEventResponse;
import com.saas.permissions.audit.domain.AuditTrailEntry;
import com.saas.permissions.shared.domain.Mapper;

@Component
public class AuditEventResponseMapper implements Mapper<AuditTrailEntry, AuditEventResponse> {

    @Override
    public AuditEventResponse map(AuditTrailEntry entry) {
        return new AuditEventResponse(
                entry.id(),
                entry.type(),
                entry.projectId(),
                entry.occurredAt(),
                entry.description());
    }
}
