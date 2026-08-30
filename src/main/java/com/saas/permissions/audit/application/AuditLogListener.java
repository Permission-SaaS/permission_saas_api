package com.saas.permissions.audit.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.saas.permissions.audit.domain.AuditEventRepository;
import com.saas.permissions.audit.domain.PermissionCheckEvent;
import com.saas.permissions.permission.domain.event.PermissionValidatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditLogListener {

    private final AuditEventRepository auditEventRepository;

    @EventListener
    public void on(PermissionValidatedEvent event) {
        auditEventRepository.save(PermissionCheckEvent.builder()
                .occurredAt(event.occurredAt())
                .routePath(event.route())
                .roleName(event.role())
                .granted(event.granted())
                .reason(event.reason())
                .durationMs(event.durationMs())
                .build());
    }
}
