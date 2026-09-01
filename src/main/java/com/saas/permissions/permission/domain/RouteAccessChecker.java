package com.saas.permissions.permission.domain;

import java.util.UUID;

import com.saas.permissions.permission.domain.dto.PermissionCheckResult;

public interface RouteAccessChecker {

    PermissionCheckResult check(UUID projectId, String role, String httpMethod, String path);
}
