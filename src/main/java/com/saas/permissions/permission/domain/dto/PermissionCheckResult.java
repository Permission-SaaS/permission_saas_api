package com.saas.permissions.permission.domain.dto;

public record PermissionCheckResult(boolean granted, String reason) {

    public static PermissionCheckResult allow() {
        return new PermissionCheckResult(true, "granted");
    }

    public static PermissionCheckResult deny(String reason) {
        return new PermissionCheckResult(false, reason);
    }
}
