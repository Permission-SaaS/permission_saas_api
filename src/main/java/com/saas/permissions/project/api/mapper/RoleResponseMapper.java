package com.saas.permissions.project.api.mapper;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.api.dto.RoleResponse;
import com.saas.permissions.project.domain.role.Role;
import com.saas.permissions.shared.domain.Mapper;

@Component
public class RoleResponseMapper implements Mapper<Role, RoleResponse> {

    @Override
    public RoleResponse map(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getProjectId(),
                role.getName(),
                role.getDescription(),
                role.isActive(),
                role.getCreatedAt() != null ? role.getCreatedAt().toString() : null,
                role.getUpdatedAt() != null ? role.getUpdatedAt().toString() : null);
    }
}
