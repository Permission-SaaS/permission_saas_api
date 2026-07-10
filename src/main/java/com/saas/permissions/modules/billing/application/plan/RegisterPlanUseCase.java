package com.saas.permissions.modules.billing.application.plan;

import org.springframework.stereotype.Service;

import com.saas.permissions.modules.billing.application.plan.command.RegisterPlanCommand;
import com.saas.permissions.modules.billing.domain.plan.Plan;
import com.saas.permissions.modules.billing.domain.plan.PlanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegisterPlanUseCase {
    private final PlanRepository planRepository;

    public Plan execute(RegisterPlanCommand command) {

        Plan plan = Plan.builder()
                .name(command.name())
                .description(command.description())
                .maxProjects(command.maxProjects())
                .maxUsersPerProject(command.maxUsersPerProject())
                .price(command.price())
                .build();

        return planRepository.save(plan);
    }
}
