package com.saas.permissions.project.application.project;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.project.ProjectRepository;
import com.saas.permissions.project.domain.role.Role;
import com.saas.permissions.project.domain.route.Route;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CheckRouteAccessUseCase {

    private final ProjectRepository projectRepository;

    public RouteAccessResult execute(UUID projectId, String roleName, String httpMethod, String path) {
        Optional<Project> project = projectRepository.findById(projectId)
                .filter(found -> found.getDeletedAt() == null)
                .filter(Project::isActive);

        if (project.isEmpty()) {
            return RouteAccessResult.PROJECT_NOT_FOUND;
        }

        Optional<Route> route = project.get().getRoutes().stream()
                .filter(candidate -> candidate.getHttpMethod().equalsIgnoreCase(httpMethod))
                .filter(candidate -> candidate.getPath().equalsIgnoreCase(path))
                .findFirst();

        if (route.isEmpty()) {
            return RouteAccessResult.ROUTE_NOT_FOUND;
        }

        if (!route.get().isActive()) {
            return RouteAccessResult.ROUTE_INACTIVE;
        }

        Optional<Role> role = project.get().getRoles().stream()
                .filter(candidate -> candidate.getName().equalsIgnoreCase(roleName))
                .findFirst();

        if (role.isEmpty()) {
            return RouteAccessResult.ROLE_NOT_FOUND;
        }

        if (!role.get().isActive()) {
            return RouteAccessResult.ROLE_INACTIVE;
        }

        if (!role.get().hasActiveAccessTo(route.get().getId())) {
            return RouteAccessResult.ROLE_HAS_NO_ACCESS_TO_ROUTE;
        }

        return RouteAccessResult.GRANTED;
    }
}
