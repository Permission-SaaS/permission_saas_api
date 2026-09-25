package com.saas.permissions.billing.api.plan;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saas.permissions.billing.api.plan.dto.PlanResponse;
import com.saas.permissions.billing.api.plan.dto.RegisterPlanRequest;
import com.saas.permissions.billing.api.plan.mapper.PlanResponseMapper;
import com.saas.permissions.billing.api.plan.mapper.RegisterPlanMapper;
import com.saas.permissions.billing.application.plan.FindAllPlansUseCase;
import com.saas.permissions.billing.application.plan.FindPlanByIdUseCase;
import com.saas.permissions.billing.application.plan.RegisterPlanUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
@Tag(name = "Plans", description = "API para gerenciamento de planos")
public class PlanController {
    private final FindPlanByIdUseCase findPlanByIdUseCase;
    private final FindAllPlansUseCase findAllPlansUseCase;
    private final RegisterPlanUseCase registerPlanUseCase;
    private final PlanResponseMapper planResponseMapper;
    private final RegisterPlanMapper registerPlanMapper;

    @GetMapping
    @Operation(summary = "Retorna todos os planos cadastrados")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Planos retornados com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<PlanResponse>> getAllPlans() {
        var plans = findAllPlansUseCase.execute();
        return ResponseEntity.ok(plans.stream().map(planResponseMapper::map).toList());
    }

    @GetMapping("/{planId}")
    @Operation(summary = "Retorna um plano pelo UUID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plano retornado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Plano não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<PlanResponse> getPlan(@PathVariable UUID planId) {
        var plan = findPlanByIdUseCase.execute(planId);
        return ResponseEntity.ok(planResponseMapper.map(plan));
    }

    @PostMapping
    @Operation(summary = "Registra um novo plano")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plano registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<PlanResponse> createPlan(@RequestBody @Valid RegisterPlanRequest request) {
        var plan = registerPlanUseCase.execute(registerPlanMapper.map(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(planResponseMapper.map(plan));
    }
}
