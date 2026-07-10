package com.saas.permissions.modules.permission.api.dto;

public record PermissionValidationResponse(boolean granted, String reason) {
}
