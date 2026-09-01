package com.saas.permissions.audit.application;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.saas.permissions.audit.domain.AuditEvent;
import com.saas.permissions.audit.domain.AuditEventJournal;
import com.saas.permissions.audit.domain.AuditEventRepository;
import com.saas.permissions.audit.domain.PermissionCheckEvent;
import com.saas.permissions.permission.domain.event.PermissionValidatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditLogListener {

    private final AuditEventRepository auditEventRepository;
    private final AuditEventJournal auditEventJournal;

    @EventListener
    public void on(PermissionValidatedEvent event) {
        AuditEvent saved = auditEventRepository.save(PermissionCheckEvent.builder()
                .projectId(event.projectId())
                .occurredAt(event.occurredAt())
                .routePath(event.route())
                .httpMethod(event.httpMethod())
                .roleName(event.role())
                .granted(event.granted())
                .reason(event.reason())
                .durationMs(event.durationMs())
                .build());

        auditEventJournal.record(saved);
    }
}
