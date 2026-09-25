package com.saas.permissions.permission.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.saas.permissions.permission.api.dto.PermissionValidationResponse;
import com.saas.permissions.permission.api.dto.ValidatePermissionRequest;
import com.saas.permissions.permission.api.mapper.PermissionValidationResponseMapper;
import com.saas.permissions.permission.api.mapper.ValidatePermissionMapper;
import com.saas.permissions.permission.application.ValidatePermissionUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Permission", description = "Validação de permissões")
public class PermissionController {

    private final ValidatePermissionUseCase validatePermissionUseCase;
    private final ValidatePermissionMapper validatePermissionMapper;
    private final PermissionValidationResponseMapper permissionValidationResponseMapper;

    @PostMapping("/validate-permission")
    @Operation(summary = "Valida se o usuario tem permissao para realizar determinada acao")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validação executada. O campo granted diz se o acesso foi concedido; reason explica a negação."),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida"),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<PermissionValidationResponse> validate(
            @RequestBody @Valid ValidatePermissionRequest request) {
        var result = validatePermissionUseCase.execute(validatePermissionMapper.map(request));
        return ResponseEntity.ok(permissionValidationResponseMapper.map(result));
    }
}
