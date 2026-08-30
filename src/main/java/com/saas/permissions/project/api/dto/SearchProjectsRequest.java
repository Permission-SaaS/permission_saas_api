package com.saas.permissions.project.api.dto;

import jakarta.validation.constraints.Size;

public record SearchProjectsRequest(

        @Size(max = 120) String name,

        Boolean onlyActive) {

}
