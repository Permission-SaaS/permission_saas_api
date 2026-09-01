package com.saas.permissions.project.application.roleroute;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.saas.permissions.project.application.project.FindProjectByIdUseCase;
import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.project.ProjectRepository;
import com.saas.permissions.project.domain.roleroute.RoleRoute;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GrantRouteToRoleUseCase {

    private final FindProjectByIdUseCase findProjectByIdUseCase;
    private final ProjectRepository projectRepository;

    public RoleRoute execute(UUID projectId, UUID roleId, UUID routeId) {
        Project project = findProjectByIdUseCase.execute(projectId);

        RoleRoute permission = project.grantRouteToRole(roleId, routeId);

        projectRepository.save(project);

        return permission;
    }
}
