package com.saas.permissions.project.api.roleroute.dto;

import java.util.UUID;

public record RolePermissionResponse(
                UUID id,
                UUID roleId,
                UUID routeId,
                boolean active,
                String grantedAt,
                String revokedAt) {

}
