package com.saas.permissions.audit.api.mapper;

import org.springframework.stereotype.Component;

import com.saas.permissions.audit.api.dto.SearchAuditEventsRequest;
import com.saas.permissions.audit.application.query.SearchAuditEventsQuery;
import com.saas.permissions.shared.domain.Mapper;

@Component
public class SearchAuditEventsMapper implements Mapper<SearchAuditEventsRequest, SearchAuditEventsQuery> {

    @Override
    public SearchAuditEventsQuery map(SearchAuditEventsRequest request) {
        return new SearchAuditEventsQuery(
                request.type(),
                request.projectId(),
                request.onlyDenied(),
                request.from(),
                request.to());
    }
}
