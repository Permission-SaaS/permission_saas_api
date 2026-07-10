package com.saas.permissions.modules.billing.api.plan.mapper;

import org.springframework.stereotype.Component;

import com.saas.permissions.modules.billing.api.plan.dto.PlanResponse;
import com.saas.permissions.modules.billing.domain.plan.Plan;
import com.saas.permissions.shared.domain.Mapper;

@Component
public class PlanResponseMapper implements Mapper<Plan, PlanResponse> {

    @Override
    public PlanResponse map(Plan plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getMaxProjects(),
                plan.getMaxUsersPerProject(),
                plan.getPrice(),
                plan.isActive());
    }
}
