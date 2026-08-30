package com.saas.permissions.audit.api.mapper;

import org.springframework.stereotype.Component;

import com.saas.permissions.audit.api.dto.SearchAuditEventsRequest;
import com.saas.permissions.audit.application.command.AuditEventQuery;
import com.saas.permissions.shared.domain.Mapper;

@Component
public class SearchAuditEventsMapper implements Mapper<SearchAuditEventsRequest, AuditEventQuery> {

    @Override
    public AuditEventQuery map(SearchAuditEventsRequest request) {
        return new AuditEventQuery(
                request.type(),
                request.projectId(),
                request.onlyDenied());
    }
}
