package com.saas.permissions.project.api.role.mapper;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.api.role.dto.RoleResponse;
import com.saas.permissions.project.api.roleroute.mapper.RolePermissionResponseMapper;
import com.saas.permissions.project.domain.role.Role;
import com.saas.permissions.shared.domain.Mapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoleResponseMapper implements Mapper<Role, RoleResponse> {

    private final RolePermissionResponseMapper rolePermissionResponseMapper;

    @Override
    public RoleResponse map(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getProjectId(),
                role.getName(),
                role.getDescription(),
                role.isActive(),
                role.activePermissions().stream().map(rolePermissionResponseMapper::map).toList(),
                role.getCreatedAt() != null ? role.getCreatedAt().toString() : null,
                role.getUpdatedAt() != null ? role.getUpdatedAt().toString() : null);
    }
}
