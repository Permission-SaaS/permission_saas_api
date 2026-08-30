package com.saas.permissions.project.api;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.saas.permissions.project.api.dto.AddRoleRequest;
import com.saas.permissions.project.api.dto.AddRouteRequest;
import com.saas.permissions.project.api.dto.CreateProjectRequest;
import com.saas.permissions.project.api.dto.ProjectResponse;
import com.saas.permissions.project.api.dto.RoleResponse;
import com.saas.permissions.project.api.dto.RouteResponse;
import com.saas.permissions.project.api.dto.SearchProjectsRequest;
import com.saas.permissions.project.api.dto.UpdateProjectRequest;
import com.saas.permissions.project.api.mapper.AddRoleToProjectMapper;
import com.saas.permissions.project.api.mapper.AddRouteToProjectMapper;
import com.saas.permissions.project.api.mapper.CreateProjectMapper;
import com.saas.permissions.project.api.mapper.ProjectResponseMapper;
import com.saas.permissions.project.api.mapper.RoleResponseMapper;
import com.saas.permissions.project.api.mapper.RouteResponseMapper;
import com.saas.permissions.project.api.mapper.SearchProjectsMapper;
import com.saas.permissions.project.api.mapper.UpdateProjectMapper;
import com.saas.permissions.project.application.AddRoleToProjectUseCase;
import com.saas.permissions.project.application.AddRouteToProjectUseCase;
import com.saas.permissions.project.application.CreateProjectUseCase;
import com.saas.permissions.project.application.DeleteProjectUseCase;
import com.saas.permissions.project.application.FindProjectByIdUseCase;
import com.saas.permissions.project.application.FindProjectRoutesUseCase;
import com.saas.permissions.project.application.SearchProjectsUseCase;
import com.saas.permissions.project.application.UpdateProjectUseCase;
import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.role.Role;
import com.saas.permissions.project.domain.route.Route;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Cadastro de projetos, cargos e rotas protegidas")
public class ProjectController {

    private final CreateProjectUseCase createProjectUseCase;
    private final UpdateProjectUseCase updateProjectUseCase;
    private final DeleteProjectUseCase deleteProjectUseCase;
    private final FindProjectByIdUseCase findProjectByIdUseCase;
    private final SearchProjectsUseCase searchProjectsUseCase;
    private final AddRoleToProjectUseCase addRoleToProjectUseCase;
    private final AddRouteToProjectUseCase addRouteToProjectUseCase;
    private final FindProjectRoutesUseCase findProjectRoutesUseCase;

    private final CreateProjectMapper createProjectMapper;
    private final UpdateProjectMapper updateProjectMapper;
    private final SearchProjectsMapper searchProjectsMapper;
    private final AddRoleToProjectMapper addRoleToProjectMapper;
    private final AddRouteToProjectMapper addRouteToProjectMapper;
    private final ProjectResponseMapper projectResponseMapper;
    private final RoleResponseMapper roleResponseMapper;
    private final RouteResponseMapper routeResponseMapper;

    @GetMapping
    @Operation(summary = "Lista os projetos, com filtro opcional por nome e por situacao")
    @ApiResponse(responseCode = "200", description = "Lista devolvida com sucesso")
    public ResponseEntity<List<ProjectResponse>> searchProjects(@Valid SearchProjectsRequest request) {

        List<Project> projects = searchProjectsUseCase.execute(searchProjectsMapper.map(request));

        return ResponseEntity.ok(projects.stream().map(projectResponseMapper::map).toList());
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Obtem um projeto pelo identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projeto encontrado"),
            @ApiResponse(responseCode = "404", description = "Projeto inexistente ou excluido")
    })
    public ResponseEntity<ProjectResponse> getProject(@PathVariable UUID projectId) {
        Project project = findProjectByIdUseCase.execute(projectId);

        return ResponseEntity.ok(projectResponseMapper.map(project));
    }

    @PostMapping
    @Operation(summary = "Cria um projeto")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Projeto criado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos")
    })
    public ResponseEntity<ProjectResponse> createProject(@RequestBody @Valid CreateProjectRequest request) {
        Project project = createProjectUseCase.execute(createProjectMapper.map(request));

        return ResponseEntity
                .created(URI.create("/projects/" + project.getId()))
                .body(projectResponseMapper.map(project));
    }

    @PutMapping("/{projectId}")
    @Operation(summary = "Altera nome, descricao e limite de cargos de um projeto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projeto alterado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "404", description = "Projeto inexistente ou excluido"),
            @ApiResponse(responseCode = "409", description = "Limite de cargos menor que os cargos ja cadastrados")
    })
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable UUID projectId,
            @RequestBody @Valid UpdateProjectRequest request) {

        Project project = updateProjectUseCase.execute(updateProjectMapper.map(projectId, request));

        return ResponseEntity.ok(projectResponseMapper.map(project));
    }

    @DeleteMapping("/{projectId}")
    @Operation(summary = "Exclui logicamente um projeto")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Projeto excluido"),
            @ApiResponse(responseCode = "404", description = "Projeto inexistente ou ja excluido")
    })
    public ResponseEntity<Void> deleteProject(@PathVariable UUID projectId) {
        deleteProjectUseCase.execute(projectId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{projectId}/roles")
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

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(roleResponseMapper.map(role));
    }

    @PostMapping("/{projectId}/routes")
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

    @GetMapping("/{projectId}/routes")
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
}
