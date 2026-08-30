package com.saas.permissions.project.api.dto;

import java.util.UUID;

public record RoleResponse(
        UUID id,
        UUID projectId,
        String name,
        String description,
        boolean active,
        String createdAt,
        String updatedAt) {

}
