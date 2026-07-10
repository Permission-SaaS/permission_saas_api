package com.saas.permissions.modules.billing.api.plan.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PlanResponse(
        UUID id,
        String name,
        int maxProjects,
        int maxUsersPerProject,
        BigDecimal price,
        boolean active) {
}
