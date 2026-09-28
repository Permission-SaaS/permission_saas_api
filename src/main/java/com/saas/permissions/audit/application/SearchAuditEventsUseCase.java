package com.saas.permissions.audit.application;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.saas.permissions.audit.application.query.SearchAuditEventsQuery;
import com.saas.permissions.audit.domain.AuditEvent;
import com.saas.permissions.audit.domain.AuditEventRepository;
import com.saas.permissions.audit.domain.ProjectLifecycleEvent;
import com.saas.permissions.audit.domain.exception.InvalidAuditPeriodException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchAuditEventsUseCase {

    private final AuditEventRepository auditEventRepository;

    public List<AuditEvent> execute(SearchAuditEventsQuery query) {
        rejectInvertedPeriod(query.from(), query.to());

        String type = normalize(query.type());

        if (Boolean.TRUE.equals(query.onlyDenied())) {
            return searchDeniedEvents(type, query);
        }

        return auditEventRepository.search(type, query.projectId(), query.from(), query.to());
    }

    private List<AuditEvent> searchDeniedEvents(String type, SearchAuditEventsQuery query) {
        if (ProjectLifecycleEvent.TYPE.equals(type)) {
            return List.of();
        }

        return auditEventRepository.searchDenied(query.projectId(), query.from(), query.to());
    }

    private void rejectInvertedPeriod(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidAuditPeriodException(from, to);
        }
    }

    private String normalize(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return type.trim().toUpperCase();
    }
}
