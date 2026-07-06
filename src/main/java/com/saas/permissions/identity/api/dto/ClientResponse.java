package com.saas.permissions.identity.api.dto;

import java.util.UUID;

public record ClientResponse(
        UUID id,
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
