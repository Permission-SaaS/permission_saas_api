package com.saas.permissions.modules.billing.api.plan;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.saas.permissions.modules.billing.api.plan.dto.PlanResponse;
import com.saas.permissions.modules.billing.api.plan.dto.RegisterPlanRequest;
import com.saas.permissions.modules.billing.api.plan.mapper.PlanResponseMapper;
import com.saas.permissions.modules.billing.api.plan.mapper.RegisterPlanMapper;
import com.saas.permissions.modules.billing.application.plan.FindAllPlansUseCase;
import com.saas.permissions.modules.billing.application.plan.FindPlanByIdUseCase;
import com.saas.permissions.modules.billing.application.plan.RegisterPlanUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class PlanController {
    private final FindPlanByIdUseCase findPlanByIdUseCase;
    private final FindAllPlansUseCase findAllPlansUseCase;
    private final RegisterPlanUseCase registerPlanUseCase;
    private final PlanResponseMapper planResponseMapper;
    private final RegisterPlanMapper registerPlanMapper;

    @GetMapping
    public ResponseEntity<List<PlanResponse>> getAllPlans() {
        var plans = findAllPlansUseCase.execute();
        return ResponseEntity.ok(plans.stream().map(planResponseMapper::map).toList());
    }

    @GetMapping("/{planId}")
    public ResponseEntity<PlanResponse> getPlan(@PathVariable UUID planId) {
        var plan = findPlanByIdUseCase.execute(planId);
        return ResponseEntity.ok(planResponseMapper.map(plan));
    }

    @PostMapping
    public ResponseEntity<PlanResponse> createPlan(@RequestBody @Valid RegisterPlanRequest request) {
        var plan = registerPlanUseCase.execute(registerPlanMapper.map(request));
        return ResponseEntity.ok(planResponseMapper.map(plan));
    }
}
