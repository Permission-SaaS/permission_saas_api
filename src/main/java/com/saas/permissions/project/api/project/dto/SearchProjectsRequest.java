package com.saas.permissions.project.api.project.dto;

import jakarta.validation.constraints.Size;

public record SearchProjectsRequest(

                @Size(max = 120) String name,

                Boolean onlyActive) {

}
