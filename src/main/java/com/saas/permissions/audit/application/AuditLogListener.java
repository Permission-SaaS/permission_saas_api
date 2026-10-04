package com.saas.permissions.audit.application;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.saas.permissions.audit.domain.AuditEventPublisher;
import com.saas.permissions.audit.domain.PermissionCheckEvent;
import com.saas.permissions.audit.domain.exception.AuditEventNotPublishedException;
import com.saas.permissions.permission.domain.event.PermissionValidatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogListener {

    private final AuditEventPublisher auditEventPublisher;

    /**
     * Roda fora da thread da validação de permissão: quem chamou a API recebe a
     * resposta sem esperar o broker, nem quando ele está fora do ar e a conexão
     * demora a falhar.
     */
    @Async
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
            auditEventPublisher.publish(check);
        } catch (AuditEventNotPublishedException e) {
            log.warn("Audit event lost: {}", e.getCause().getMessage());
        }
    }

}
