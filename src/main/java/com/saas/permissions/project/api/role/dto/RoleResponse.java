package com.saas.permissions.project.api.role.dto;

import java.util.List;
import java.util.UUID;

import com.saas.permissions.project.api.roleroute.dto.RolePermissionResponse;

public record RoleResponse(
                UUID id,
                UUID projectId,
                String name,
                String description,
                boolean active,
                List<RolePermissionResponse> permissions,
                String createdAt,
                String updatedAt) {

}
