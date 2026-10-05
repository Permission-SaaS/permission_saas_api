package com.saas.permissions.project.api.route.dto;

public record RouteImportResponse(
        long executionId,
        String status,
        long read,
        long imported,
        long discarded) {
}
