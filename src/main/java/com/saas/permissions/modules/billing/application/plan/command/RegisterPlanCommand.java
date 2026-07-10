package com.saas.permissions.modules.billing.application.plan.command;

import java.math.BigDecimal;

public record RegisterPlanCommand(
                String name,
                String description,
                int maxProjects,
                int maxUsersPerProject,
                BigDecimal price) {
}
