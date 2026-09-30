package com.saas.permissions.audit.infrastructure.client;

import org.springframework.stereotype.Component;

import com.saas.permissions.audit.domain.AuditTrail;
import com.saas.permissions.audit.domain.PermissionCheckEvent;
import com.saas.permissions.audit.domain.exception.AuditTrailUnavailableException;
import com.saas.permissions.audit.infrastructure.client.dto.RegisterPermissionCheckRequest;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditTrailClientAdapter implements AuditTrail {

    private final AuditClient auditClient;

    @Override
    public void record(PermissionCheckEvent event) {
        RegisterPermissionCheckRequest request = new RegisterPermissionCheckRequest(
                event.getProjectId(),
                event.getOccurredAt(),
                event.getRoutePath(),
                event.getHttpMethod(),
                event.getRoleName(),
                event.isGranted(),
                event.getReason(),
                event.getDurationMs());

        try {
            auditClient.registerPermissionCheck(request);
        } catch (FeignException e) {
            throw new AuditTrailUnavailableException(e);
        }
    }
}
