package com.saas.permissions.audit.application;

import java.util.List;

import org.springframework.stereotype.Service;

import com.saas.permissions.audit.application.command.AuditEventQuery;
import com.saas.permissions.audit.domain.AuditEvent;
import com.saas.permissions.audit.domain.AuditEventRepository;
import com.saas.permissions.audit.domain.PermissionCheckEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindAuditEventsUseCase {

    private final AuditEventRepository auditEventRepository;

    public List<AuditEvent> execute(AuditEventQuery query) {
        return findByProject(query).stream()
                .filter(event -> matchesType(event, query.type()))
                .filter(event -> matchesDenied(event, query.onlyDenied()))
                .toList();
    }

    private List<AuditEvent> findByProject(AuditEventQuery query) {
        if (query.projectId() == null) {
            return auditEventRepository.findAll();
        }

        return auditEventRepository.findAllByProjectId(query.projectId());
    }

    private boolean matchesType(AuditEvent event, String type) {
        if (type == null || type.isBlank()) {
            return true;
        }

        return event.type().equalsIgnoreCase(type);
    }

    private boolean matchesDenied(AuditEvent event, Boolean onlyDenied) {
        if (onlyDenied == null || !onlyDenied) {
            return true;
        }

        return event instanceof PermissionCheckEvent check && !check.isGranted();
    }
}
