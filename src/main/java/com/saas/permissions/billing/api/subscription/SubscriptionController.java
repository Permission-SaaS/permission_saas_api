package com.saas.permissions.billing.api.subscription;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saas.permissions.billing.api.subscription.dto.SubscribeToPlanRequest;
import com.saas.permissions.billing.api.subscription.dto.SubscriptionResponse;
import com.saas.permissions.billing.api.subscription.mapper.SubscribeToPlanMapper;
import com.saas.permissions.billing.api.subscription.mapper.SubscriptionResponseMapper;
import com.saas.permissions.billing.application.subscription.SubscribeToPlanUseCase;
import com.saas.permissions.shared.api.dto.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscription", description = "API para gerenciamento de assinaturas")
public class SubscriptionController {

    private final SubscribeToPlanUseCase subscribeToPlanUseCase;
    private final SubscribeToPlanMapper subscribeToPlanMapper;
    private final SubscriptionResponseMapper subscriptionResponseMapper;

    @PostMapping
    @Operation(summary = "Assina um plano para um cliente ja cadastrado, cobra o pagamento e gera a ApiKey")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Assinatura criada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida"),
            @ApiResponse(responseCode = "404", description = "Cliente ou plano nao encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                    @ExampleObject(name = "Cliente nao encontrado", value = "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Client not found\",\"timestamp\":\"2026-09-25T12:00:00Z\"}"),
                    @ExampleObject(name = "Plano nao encontrado", value = "{\"status\":404,\"error\":\"Not Found\",\"message\":\"Plan not found\",\"timestamp\":\"2026-09-25T12:00:00Z\"}")
            })),
            @ApiResponse(responseCode = "409", description = "Assinatura ativa ja existente para o mesmo plano, ou pagamento recusado", content = @Content(schema = @Schema(implementation = ErrorResponse.class), examples = {
                    @ExampleObject(name = "Assinatura ativa", value = "{\"status\":409,\"error\":\"Conflict\",\"message\":\"Client already has an active subscription to this plan, valid until 2026-10-25T12:00:00Z\",\"timestamp\":\"2026-09-25T12:00:00Z\"}"),
                    @ExampleObject(name = "Pagamento recusado", value = "{\"status\":409,\"error\":\"Conflict\",\"message\":\"Payment declined for subscription b1a2c3d4-0000-0000-0000-000000000000\",\"timestamp\":\"2026-09-25T12:00:00Z\"}")
            })),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<SubscriptionResponse> subscribe(@RequestBody @Valid SubscribeToPlanRequest request) {
        var result = subscribeToPlanUseCase.execute(subscribeToPlanMapper.map(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionResponseMapper.map(result));
    }
}
