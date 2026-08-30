package com.saas.permissions.project.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(

        @Size(max = 120) String name,

        @Size(max = 500) String description,

        @Min(1) Integer maxRoles) {

}
