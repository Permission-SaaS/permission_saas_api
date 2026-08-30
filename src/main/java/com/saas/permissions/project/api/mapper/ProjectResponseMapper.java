package com.saas.permissions.project.api.mapper;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.api.dto.ProjectResponse;
import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.shared.domain.Mapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectResponseMapper implements Mapper<Project, ProjectResponse> {

    private final RoleResponseMapper roleResponseMapper;

    private final RouteResponseMapper routeResponseMapper;

    @Override
    public ProjectResponse map(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getClientId(),
                project.getName(),
                project.getDescription(),
                project.getMaxRoles(),
                project.isActive(),
                project.getCreatedAt() != null ? project.getCreatedAt().toString() : null,
                project.getUpdatedAt() != null ? project.getUpdatedAt().toString() : null,
                project.getRoles().stream().map(roleResponseMapper::map).toList(),
                project.getRoutes().stream().map(routeResponseMapper::map).toList());
    }
}
