package com.saas.permissions.audit.infrastructure.client;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.saas.permissions.audit.domain.AuditTrail;
import com.saas.permissions.audit.domain.AuditTrailEntry;
import com.saas.permissions.audit.domain.exception.AuditTrailUnavailableException;
import com.saas.permissions.audit.infrastructure.client.dto.AuditEventResponse;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditTrailClientAdapter implements AuditTrail {

    private final AuditClient auditClient;

    @Override
    public List<AuditTrailEntry> search(UUID projectId, String type, Boolean onlyDenied, OffsetDateTime from,
            OffsetDateTime to) {

        try {
            List<AuditEventResponse> response = auditClient.searchAuditEvents(projectId, type, onlyDenied,
                    from != null ? from.toInstant() : null, to != null ? to.toInstant() : null);

            return response.stream().map(this::toAuditTrailEntry).toList();
        } catch (FeignException e) {
            throw new AuditTrailUnavailableException(e);
        }

    }

    private AuditTrailEntry toAuditTrailEntry(AuditEventResponse event) {
        return new AuditTrailEntry(event.id(), event.type(), event.projectId(), event.occurredAt(),
                event.description());
    }
}
