package com.saas.permissions.billing.application.plan;

import java.util.List;

import org.springframework.stereotype.Service;

import com.saas.permissions.billing.domain.plan.Plan;
import com.saas.permissions.billing.domain.plan.PlanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindAllPlansUseCase {
    private final PlanRepository planRepository;

    public List<Plan> execute() {
        return planRepository.findAll();
    }
}
