package com.saas.permissions.project.api.roleroute.mapper;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.api.roleroute.dto.RolePermissionResponse;
import com.saas.permissions.project.domain.roleroute.RoleRoute;
import com.saas.permissions.shared.domain.Mapper;

@Component
public class RolePermissionResponseMapper implements Mapper<RoleRoute, RolePermissionResponse> {

    @Override
    public RolePermissionResponse map(RoleRoute permission) {
        return new RolePermissionResponse(
                permission.getId(),
                permission.getRoleId(),
                permission.getRouteId(),
                permission.isActive(),
                permission.getGrantedAt() != null ? permission.getGrantedAt().toString() : null,
                permission.getRevokedAt() != null ? permission.getRevokedAt().toString() : null);
    }
}
