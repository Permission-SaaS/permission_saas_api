package com.saas.permissions.identity.application.command;

import com.saas.permissions.identity.domain.AuthProvider;

public record RegisterClientCommand(
        String name,
        String email,
        String phone,
        String rawPassword,
        AuthProvider provider,
        String providerId
) {}
