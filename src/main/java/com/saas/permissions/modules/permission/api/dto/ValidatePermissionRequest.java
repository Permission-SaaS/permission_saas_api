package com.saas.permissions.modules.permission.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidatePermissionRequest(

        @NotBlank String apiKey,

        @NotBlank String role,

        @NotBlank String route) {
}
