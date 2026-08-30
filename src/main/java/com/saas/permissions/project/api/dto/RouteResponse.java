package com.saas.permissions.project.api.dto;

import java.util.UUID;

public record RouteResponse(
        UUID id,
        UUID projectId,
        String name,
        String httpMethod,
        String path,
        String description,
        boolean active,
        String createdAt,
        String updatedAt) {

}
