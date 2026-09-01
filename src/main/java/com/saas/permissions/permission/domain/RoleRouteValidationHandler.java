package com.saas.permissions.permission.domain;

import org.springframework.stereotype.Component;

import com.saas.permissions.permission.domain.dto.PermissionCheckRequest;
import com.saas.permissions.permission.domain.dto.PermissionCheckResult;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoleRouteValidationHandler extends PermissionValidationHandler {

    private final RouteAccessChecker routeAccessChecker;

    @Override
    protected PermissionCheckResult check(PermissionCheckRequest request) {
        return routeAccessChecker.check(
                request.projectId(),
                request.role(),
                request.httpMethod(),
                request.route());
    }
}
