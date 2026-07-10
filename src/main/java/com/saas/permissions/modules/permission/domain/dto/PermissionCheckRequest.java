package com.saas.permissions.permission.domain.dto;

public record PermissionCheckRequest(String apiKey, String role, String route) {
}
