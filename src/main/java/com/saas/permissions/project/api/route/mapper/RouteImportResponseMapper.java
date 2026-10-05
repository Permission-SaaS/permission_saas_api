package com.saas.permissions.project.api.route.mapper;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.api.route.dto.RouteImportResponse;
import com.saas.permissions.project.domain.route.RouteImportResult;
import com.saas.permissions.shared.domain.Mapper;

@Component
public class RouteImportResponseMapper implements Mapper<RouteImportResult, RouteImportResponse> {

    @Override
    public RouteImportResponse map(RouteImportResult result) {
        return new RouteImportResponse(
                result.executionId(),
                result.status(),
                result.read(),
                result.imported(),
                result.discarded());
    }
}
