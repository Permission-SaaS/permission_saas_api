package com.saas.permissions.audit.application;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.saas.permissions.audit.application.command.AuditEventQuery;
import com.saas.permissions.audit.domain.AuditEvent;
import com.saas.permissions.audit.domain.AuditEventRepository;
import com.saas.permissions.audit.domain.PermissionCheckEvent;

import lombok.RequiredArgsConstructor;

/**
 * Le a trilha de auditoria aplicando filtros opcionais, do evento mais recente
 * para o mais antigo.
 */
@Service
@RequiredArgsConstructor
public class FindAuditEventsUseCase {

    private final AuditEventRepository auditEventRepository;

    public List<AuditEvent> execute(AuditEventQuery query) {
        return auditEventRepository.findAll().stream()
                .filter(event -> matchesType(event, query.type()))
                .filter(event -> matchesProject(event, query))
                .filter(event -> matchesDenied(event, query.onlyDenied()))
                .sorted(Comparator.comparing(AuditEvent::getOccurredAt).reversed())
                .toList();
    }

    private boolean matchesType(AuditEvent event, String type) {
        if (type == null || type.isBlank()) {
            return true;
        }

        return event.type().equalsIgnoreCase(type);
    }

    private boolean matchesProject(AuditEvent event, AuditEventQuery query) {
        if (query.projectId() == null) {
            return true;
        }

        return query.projectId().equals(event.getProjectId());
    }

    /**
     * Filtro de negocio mais util da trilha: so as tentativas negadas. So faz
     * sentido para a checagem de permissao, entao os demais tipos saem da lista.
     */
    private boolean matchesDenied(AuditEvent event, Boolean onlyDenied) {
        if (onlyDenied == null || !onlyDenied) {
            return true;
        }

        return event instanceof PermissionCheckEvent check && !check.isGranted();
    }
}
