package com.saas.permissions.project.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddRoleRequest(

        @NotBlank @Size(max = 80) String name,

        @Size(max = 255) String description) {

}
