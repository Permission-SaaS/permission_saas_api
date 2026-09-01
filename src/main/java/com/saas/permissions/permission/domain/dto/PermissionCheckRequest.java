package com.saas.permissions.permission.domain.dto;

import java.util.UUID;

public record PermissionCheckRequest(
        String apiKey,
        UUID projectId,
        String role,
        String httpMethod,
        String route) {
}
