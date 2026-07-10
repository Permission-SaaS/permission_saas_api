package com.saas.permissions.permission.api.dto;

public record PermissionValidationResponse(boolean granted, String reason) {
}
