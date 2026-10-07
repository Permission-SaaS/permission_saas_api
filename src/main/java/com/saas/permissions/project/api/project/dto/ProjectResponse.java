package com.saas.permissions.project.api.project.dto;

import com.saas.permissions.project.api.role.dto.RoleResponse;
import com.saas.permissions.project.api.route.dto.RouteResponse;

import java.util.List;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID clientId,
        String name,
        String description,
        Integer maxRoles,
        boolean active,
        String createdAt,
        String updatedAt,
        List<RoleResponse> roles,
        List<RouteResponse> routes) {

}
