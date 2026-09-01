package com.saas.permissions.project.infrastructure.route;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.domain.route.Route;

@Component
public class RouteJpaMapper {

    public RouteJpaEntity toJpa(Route route) {
        return RouteJpaEntity.builder()
                .id(route.getId())
                .name(route.getName())
                .httpMethod(route.getHttpMethod())
                .path(route.getPath())
                .description(route.getDescription())
                .isActive(route.isActive())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .build();
    }

    public Route toDomain(RouteJpaEntity entity, UUID projectId) {
        return Route.builder()
                .id(entity.getId())
                .projectId(projectId)
                .name(entity.getName())
                .httpMethod(entity.getHttpMethod())
                .path(entity.getPath())
                .description(entity.getDescription())
                .isActive(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void copy(Route route, RouteJpaEntity target) {
        target.setName(route.getName());
        target.setHttpMethod(route.getHttpMethod());
        target.setPath(route.getPath());
        target.setDescription(route.getDescription());
        target.setActive(route.isActive());
        target.setUpdatedAt(route.getUpdatedAt());
    }
}
