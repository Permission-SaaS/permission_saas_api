package com.saas.permissions.project.api.route;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saas.permissions.project.api.route.dto.AddRouteRequest;
import com.saas.permissions.project.api.route.dto.RouteImportRequest;
import com.saas.permissions.project.api.route.dto.RouteImportResponse;
import com.saas.permissions.project.api.route.dto.RouteResponse;
import com.saas.permissions.project.api.route.mapper.AddRouteToProjectMapper;
import com.saas.permissions.project.api.route.mapper.RouteImportResponseMapper;
import com.saas.permissions.project.api.route.mapper.RouteResponseMapper;
import com.saas.permissions.project.application.route.AddRouteToProjectUseCase;
import com.saas.permissions.project.application.route.FindProjectRoutesUseCase;
import com.saas.permissions.project.application.route.ImportRoutesUseCase;
import com.saas.permissions.project.domain.route.Route;
import com.saas.permissions.project.domain.route.RouteImportResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects/{projectId}/routes")
@RequiredArgsConstructor
@Tag(name = "Routes", description = "Rotas protegidas de um projeto")
public class RouteController {

    private final AddRouteToProjectUseCase addRouteToProjectUseCase;
    private final FindProjectRoutesUseCase findProjectRoutesUseCase;
    private final ImportRoutesUseCase importRoutesUseCase;

    private final AddRouteToProjectMapper addRouteToProjectMapper;
    private final RouteResponseMapper routeResponseMapper;
    private final RouteImportResponseMapper routeImportResponseMapper;

    @PostMapping
    @Operation(summary = "Adiciona uma rota ao projeto")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Rota adicionada"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "404", description = "Projeto inexistente ou excluido"),
            @ApiResponse(responseCode = "409", description = "Rota duplicada para o mesmo metodo HTTP")
    })
    public ResponseEntity<RouteResponse> addRoute(
            @PathVariable UUID projectId,
            @RequestBody @Valid AddRouteRequest request) {

        Route route = addRouteToProjectUseCase.execute(addRouteToProjectMapper.map(projectId, request));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(routeResponseMapper.map(route));
    }

    @GetMapping
    @Operation(summary = "Lista as rotas do projeto, com filtro opcional por metodo HTTP")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista devolvida com sucesso"),
            @ApiResponse(responseCode = "404", description = "Projeto inexistente ou excluido")
    })
    public ResponseEntity<List<RouteResponse>> getProjectRoutes(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String httpMethod) {

        List<Route> routes = findProjectRoutesUseCase.execute(projectId, httpMethod);

        return ResponseEntity.ok(routes.stream().map(routeResponseMapper::map).toList());
    }

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importa rotas em lote a partir de um CSV (name,httpMethod,path,description), com Spring Batch")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Importacao executada; o corpo traz o resumo"),
            @ApiResponse(responseCode = "400", description = "Arquivo ausente"),
            @ApiResponse(responseCode = "404", description = "Projeto inexistente ou excluido")
    })
    public ResponseEntity<RouteImportResponse> importRoutes(
            @PathVariable UUID projectId,
            @Valid @ModelAttribute RouteImportRequest request) throws IOException {

        RouteImportResult result = importRoutesUseCase.execute(projectId, request.file().getInputStream());

        return ResponseEntity.ok(routeImportResponseMapper.map(result));
    }
}
