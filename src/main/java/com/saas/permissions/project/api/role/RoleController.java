package com.saas.permissions.project.api.role;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saas.permissions.project.api.role.dto.AddRoleRequest;
import com.saas.permissions.project.api.role.dto.RoleResponse;
import com.saas.permissions.project.api.role.mapper.AddRoleToProjectMapper;
import com.saas.permissions.project.api.role.mapper.RoleResponseMapper;
import com.saas.permissions.project.application.role.AddRoleToProjectUseCase;
import com.saas.permissions.project.domain.role.Role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects/{projectId}/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Cargos de um projeto")
public class RoleController {

        private final AddRoleToProjectUseCase addRoleToProjectUseCase;

        private final AddRoleToProjectMapper addRoleToProjectMapper;
        private final RoleResponseMapper roleResponseMapper;

        @PostMapping
        @Operation(summary = "Adiciona um cargo ao projeto")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Cargo adicionado"),
                        @ApiResponse(responseCode = "400", description = "Dados invalidos"),
                        @ApiResponse(responseCode = "404", description = "Projeto inexistente ou excluido"),
                        @ApiResponse(responseCode = "409", description = "Cargo duplicado ou limite do plano excedido")
        })
        public ResponseEntity<RoleResponse> addRole(
                        @PathVariable UUID projectId,
                        @RequestBody @Valid AddRoleRequest request) {

                Role role = addRoleToProjectUseCase.execute(addRoleToProjectMapper.map(projectId, request));

                return ResponseEntity.status(HttpStatus.CREATED).body(roleResponseMapper.map(role));
        }
}
