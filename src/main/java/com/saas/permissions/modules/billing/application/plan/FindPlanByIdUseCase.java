package com.saas.permissions.billing.application.plan;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.saas.permissions.billing.domain.plan.Plan;
import com.saas.permissions.billing.domain.plan.PlanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindPlanByIdUseCase {
    private final PlanRepository planRepository;

    public Plan execute(UUID planId) {
        Optional<Plan> foundPlan = planRepository.findById(planId);

        if (foundPlan.isEmpty()) {
            throw new RuntimeException("Plan not found");
        }

        return foundPlan.get();
    }
}
