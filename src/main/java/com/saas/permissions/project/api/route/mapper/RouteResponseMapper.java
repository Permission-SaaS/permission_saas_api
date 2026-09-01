package com.saas.permissions.project.api.route.mapper;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.api.route.dto.RouteResponse;
import com.saas.permissions.project.domain.route.Route;
import com.saas.permissions.shared.domain.Mapper;

@Component
public class RouteResponseMapper implements Mapper<Route, RouteResponse> {

    @Override
    public RouteResponse map(Route route) {
        return new RouteResponse(
                route.getId(),
                route.getProjectId(),
                route.getName(),
                route.getHttpMethod(),
                route.getPath(),
                route.getDescription(),
                route.isActive(),
                route.getCreatedAt() != null ? route.getCreatedAt().toString() : null,
                route.getUpdatedAt() != null ? route.getUpdatedAt().toString() : null);
    }
}
