package com.saas.permissions.audit.application;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.saas.permissions.audit.application.query.SearchAuditEventsQuery;
import com.saas.permissions.audit.domain.AuditTrail;
import com.saas.permissions.audit.domain.AuditTrailEntry;
import com.saas.permissions.audit.domain.exception.InvalidAuditPeriodException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchAuditEventsUseCase {

    private final AuditTrail auditTrail;

    public List<AuditTrailEntry> execute(SearchAuditEventsQuery query) {
        rejectInvertedPeriod(query.from(), query.to());

        return auditTrail.search(query.projectId(), query.type(), query.onlyDenied(), query.from(),
                query.to());
    }

    private void rejectInvertedPeriod(OffsetDateTime from, OffsetDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidAuditPeriodException(from, to);
        }
    }

}
