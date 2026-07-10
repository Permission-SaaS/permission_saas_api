package com.saas.permissions.modules.permission.domain.dto;

public record PermissionCheckRequest(String apiKey, String role, String route) {
}
