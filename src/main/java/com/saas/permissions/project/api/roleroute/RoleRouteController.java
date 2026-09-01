package com.saas.permissions.project.api.roleroute;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saas.permissions.project.api.roleroute.dto.RolePermissionResponse;
import com.saas.permissions.project.api.roleroute.mapper.RolePermissionResponseMapper;
import com.saas.permissions.project.application.roleroute.FindRolePermissionsUseCase;
import com.saas.permissions.project.application.roleroute.GrantRouteToRoleUseCase;
import com.saas.permissions.project.application.roleroute.RevokeRouteFromRoleUseCase;
import com.saas.permissions.project.domain.roleroute.RoleRoute;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects/{projectId}/roles/{roleId}/routes")
@RequiredArgsConstructor
@Tag(name = "Role permissions", description = "Quais rotas cada cargo pode acessar, com historico de concessao e revogacao")
public class RoleRouteController {

        private final GrantRouteToRoleUseCase grantRouteToRoleUseCase;
        private final RevokeRouteFromRoleUseCase revokeRouteFromRoleUseCase;
        private final FindRolePermissionsUseCase findRolePermissionsUseCase;

        private final RolePermissionResponseMapper rolePermissionResponseMapper;

        @PostMapping("/{routeId}")
        @Operation(summary = "Concede ao cargo o acesso a uma rota do projeto")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Acesso concedido"),
                        @ApiResponse(responseCode = "404", description = "Projeto, cargo ou rota inexistente"),
                        @ApiResponse(responseCode = "409", description = "O cargo ja possui concessao ativa nessa rota")
        })
        public ResponseEntity<RolePermissionResponse> grant(
                        @PathVariable UUID projectId,
                        @PathVariable UUID roleId,
                        @PathVariable UUID routeId) {

                RoleRoute permission = grantRouteToRoleUseCase.execute(projectId, roleId, routeId);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(rolePermissionResponseMapper.map(permission));
        }

        @DeleteMapping("/{routeId}")
        @Operation(summary = "Revoga o acesso do cargo a uma rota, preservando o registro historico")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Acesso revogado"),
                        @ApiResponse(responseCode = "404", description = "Projeto, cargo, rota ou concessao ativa inexistente")
        })
        public ResponseEntity<Void> revoke(
                        @PathVariable UUID projectId,
                        @PathVariable UUID roleId,
                        @PathVariable UUID routeId) {

                revokeRouteFromRoleUseCase.execute(projectId, roleId, routeId);

                return ResponseEntity.noContent().build();
        }

        @GetMapping
        @Operation(summary = "Lista as rotas que o cargo pode acessar; com includeRevoked=true devolve tambem o historico")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lista devolvida com sucesso"),
                        @ApiResponse(responseCode = "404", description = "Projeto ou cargo inexistente")
        })
        public ResponseEntity<List<RolePermissionResponse>> list(
                        @PathVariable UUID projectId,
                        @PathVariable UUID roleId,
                        @RequestParam(required = false, defaultValue = "false") boolean includeRevoked) {

                List<RoleRoute> permissions = findRolePermissionsUseCase.execute(projectId, roleId, includeRevoked);

                return ResponseEntity.ok(permissions.stream().map(rolePermissionResponseMapper::map).toList());
        }
}
