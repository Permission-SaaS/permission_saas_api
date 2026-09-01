package com.saas.permissions.permission.infrastructure;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.saas.permissions.permission.domain.RouteAccessChecker;
import com.saas.permissions.permission.domain.dto.PermissionCheckResult;
import com.saas.permissions.project.application.project.CheckRouteAccessUseCase;
import com.saas.permissions.project.application.project.RouteAccessResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectRouteAccessChecker implements RouteAccessChecker {

    private final CheckRouteAccessUseCase checkRouteAccessUseCase;

    @Override
    public PermissionCheckResult check(UUID projectId, String role, String httpMethod, String path) {
        RouteAccessResult result = checkRouteAccessUseCase.execute(projectId, role, httpMethod, path);

        return switch (result) {
            case GRANTED -> PermissionCheckResult.allow();
            case PROJECT_NOT_FOUND -> PermissionCheckResult.deny("project not found or inactive");
            case ROUTE_NOT_FOUND -> PermissionCheckResult.deny("route not registered in the project");
            case ROUTE_INACTIVE -> PermissionCheckResult.deny("route is inactive");
            case ROLE_NOT_FOUND -> PermissionCheckResult.deny("role not registered in the project");
            case ROLE_INACTIVE -> PermissionCheckResult.deny("role is inactive");
            case ROLE_HAS_NO_ACCESS_TO_ROUTE -> PermissionCheckResult.deny("role has no active grant on this route");
        };
    }
}
