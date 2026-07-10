package com.saas.permissions.modules.identity.application.command;

import com.saas.permissions.modules.identity.domain.AuthProvider;

public record RegisterClientCommand(
                String name,
                String email,
                String phone,
                String rawPassword,
                AuthProvider provider,
                String providerId) {
}
