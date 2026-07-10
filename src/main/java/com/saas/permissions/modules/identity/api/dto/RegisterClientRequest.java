package com.saas.permissions.modules.identity.api.dto;

import com.saas.permissions.modules.identity.domain.AuthProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterClientRequest(

        @NotBlank String name,

        @NotBlank @Email String email,

        String phone,

        String rawPassword,

        AuthProvider provider,

        String providerId
) {}
