package com.saas.permissions.modules.billing.api.plan.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RegisterPlanRequest(
                @NotBlank String name,

                String description,

                @Positive int maxProjects,

                @Positive int maxUsersPerProject,

                @NotNull @Positive BigDecimal price) {

}
