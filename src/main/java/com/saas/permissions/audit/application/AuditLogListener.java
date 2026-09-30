package com.saas.permissions.audit.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.saas.permissions.audit.domain.AuditTrail;
import com.saas.permissions.audit.domain.PermissionCheckEvent;
import com.saas.permissions.audit.domain.exception.AuditTrailUnavailableException;
import com.saas.permissions.permission.domain.event.PermissionValidatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogListener {

    private final AuditTrail auditTrail;

    @EventListener
    public void on(PermissionValidatedEvent event) {
        PermissionCheckEvent check = PermissionCheckEvent.builder()
                .projectId(event.projectId())
                .occurredAt(event.occurredAt())
                .routePath(event.route())
                .httpMethod(event.httpMethod())
                .roleName(event.role())
                .granted(event.granted())
                .reason(event.reason())
                .durationMs(event.durationMs())
                .build();

        try {
            auditTrail.record(check);
        } catch (AuditTrailUnavailableException e) {
            log.warn("Audit event lost: {}", e.getCause().getMessage());
        }
    }

}
