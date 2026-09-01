package com.saas.permissions.permission.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ValidatePermissionRequest(

        @NotBlank String apiKey,

        @NotNull UUID projectId,

        @NotBlank @Size(max = 80) String role,

        @NotBlank @Pattern(regexp = "(?i)GET|POST|PUT|PATCH|DELETE", message = "must be a valid HTTP method") String httpMethod,

        @NotBlank @Size(max = 255) @Pattern(regexp = "^/.*", message = "must start with /") String route) {
}
