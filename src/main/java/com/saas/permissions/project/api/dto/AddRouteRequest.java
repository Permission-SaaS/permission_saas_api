package com.saas.permissions.project.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddRouteRequest(

        @NotBlank @Size(max = 80) String name,

        @NotBlank @Pattern(regexp = "^/.*", message = "must start with '/'") @Size(max = 255) String path,

        @NotBlank @Pattern(regexp = "(?i)GET|POST|PUT|PATCH|DELETE", message = "must be a valid HTTP method") String httpMethod,

        @Size(max = 255) String description) {

}
