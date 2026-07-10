package com.saas.permissions.modules.identity.domain;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Client {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String passwordHash;
    private AuthProvider provider;
    private String providerId;

    @Builder.Default
    private ClientStatus status = ClientStatus.active;

    @Builder.Default
    private boolean emailVerified = false;

    @Builder.Default
    private boolean blocked = false;

    @Builder.Default
    private int loginAttempts = 0;

    private OffsetDateTime blockExpiresAt;
    private OffsetDateTime emailVerifiedAt;
    private OffsetDateTime lastLoginAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}
