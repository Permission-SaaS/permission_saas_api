package com.saas.permissions.identity.api.dto;

public record ClientResponse(
        String name,
        String email,
        String phone,
        String provider,
        String providerId,
        String status,
        boolean emailVerified,
        boolean blocked,
        int loginAttempts,
        String blockExpiresAt,
        String emailVerifiedAt,
        String lastLoginAt,
        String createdAt,
        String updatedAt,
        String deletedAt) {

}
