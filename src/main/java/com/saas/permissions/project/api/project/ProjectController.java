package com.saas.permissions.project.api.project;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saas.permissions.project.api.project.dto.CreateProjectRequest;
import com.saas.permissions.project.api.project.dto.ProjectResponse;
import com.saas.permissions.project.api.project.dto.SearchProjectsRequest;
import com.saas.permissions.project.api.project.dto.UpdateProjectRequest;
import com.saas.permissions.project.api.project.mapper.CreateProjectMapper;
import com.saas.permissions.project.api.project.mapper.ProjectResponseMapper;
import com.saas.permissions.project.api.project.mapper.SearchProjectsMapper;
import com.saas.permissions.project.api.project.mapper.UpdateProjectMapper;
import com.saas.permissions.project.application.project.CreateProjectUseCase;
import com.saas.permissions.project.application.project.DeleteProjectUseCase;
import com.saas.permissions.project.application.project.FindProjectByIdUseCase;
import com.saas.permissions.project.application.project.PurgeProjectUseCase;
import com.saas.permissions.project.application.project.SearchProjectsUseCase;
import com.saas.permissions.project.application.project.UpdateProjectUseCase;
import com.saas.permissions.project.domain.project.Project;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Cadastro de projetos")
public class ProjectController {

    private final CreateProjectUseCase createProjectUseCase;
    private final UpdateProjectUseCase updateProjectUseCase;
    private final DeleteProjectUseCase deleteProjectUseCase;
    private final PurgeProjectUseCase purgeProjectUseCase;
    private final FindProjectByIdUseCase findProjectByIdUseCase;
    private final SearchProjectsUseCase searchProjectsUseCase;

    private final CreateProjectMapper createProjectMapper;
    private final UpdateProjectMapper updateProjectMapper;
    private final SearchProjectsMapper searchProjectsMapper;
    private final ProjectResponseMapper projectResponseMapper;

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

    @DeleteMapping("/{projectId}/purge")
    @Operation(summary = "Remove definitivamente um projeto e, em cascata, seus cargos e rotas")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Projeto removido definitivamente"),
            @ApiResponse(responseCode = "404", description = "Projeto inexistente")
    })
    public ResponseEntity<Void> purgeProject(@PathVariable UUID projectId) {
        purgeProjectUseCase.execute(projectId);

        return ResponseEntity.noContent().build();
    }
}
