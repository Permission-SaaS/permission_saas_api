package com.saas.permissions.project.application.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.saas.permissions.project.application.project.query.SearchProjectsQuery;
import com.saas.permissions.project.application.route.FindProjectRoutesUseCase;
import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.project.ProjectRepository;
import com.saas.permissions.project.domain.project.exception.ProjectNotFoundException;
import com.saas.permissions.project.domain.route.Route;

@DisplayName("Casos de uso de Project sobre um repositorio falso")
class ProjectUseCasesTest {

    private static class FakeProjectRepository implements ProjectRepository {

        private final Map<UUID, Project> projects = new LinkedHashMap<>();

        @Override
        public Project save(Project project) {
            projects.put(project.getId(), project);
            return project;
        }

        @Override
        public Optional<Project> findById(UUID id) {
            return Optional.ofNullable(projects.get(id));
        }

        @Override
        public List<Project> findAll() {
            return new ArrayList<>(projects.values());
        }

        @Override
        public List<Project> findAllActive() {
            return projects.values().stream()
                    .filter(project -> project.getDeletedAt() == null)
                    .sorted(Comparator.comparing(Project::getCreatedAt))
                    .toList();
        }

        @Override
        public List<Project> searchByName(String name) {
            String term = name == null ? "" : name.strip().toLowerCase();

            return projects.values().stream()
                    .filter(project -> project.getDeletedAt() == null)
                    .filter(project -> project.getName().toLowerCase().contains(term))
                    .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }

        @Override
        public List<Project> findAllByClientId(UUID clientId) {
            return projects.values().stream()
                    .filter(project -> project.getDeletedAt() == null)
                    .filter(project -> clientId.equals(project.getClientId()))
                    .toList();
        }

        @Override
        public boolean existsById(UUID id) {
            return projects.containsKey(id);
        }

        @Override
        public void deleteById(UUID id) {
            projects.remove(id);
        }
    }

    private FakeProjectRepository repository;
    private FindProjectByIdUseCase findProjectById;
    private DeleteProjectUseCase deleteProject;
    private SearchProjectsUseCase searchProjects;
    private FindProjectRoutesUseCase findProjectRoutes;

    @BeforeEach
    void setUp() {
        repository = new FakeProjectRepository();
        findProjectById = new FindProjectByIdUseCase(repository);
        deleteProject = new DeleteProjectUseCase(findProjectById, repository);
        searchProjects = new SearchProjectsUseCase(repository);
        findProjectRoutes = new FindProjectRoutesUseCase(findProjectById);
    }

    private Project persist(String name, boolean active) {
        Project project = Project.builder()
                .name(name)
                .description("Descricao de " + name)
                .maxRoles(5)
                .build();

        if (!active) {
            project.deactivate();
        }

        return repository.save(project);
    }

    @Nested
    @DisplayName("DeleteProjectUseCase")
    class Delete {

        @Test
        @DisplayName("deve marcar o projeto como excluido sem remove-lo do Map")
        void shouldSoftDelete() {
            // ARRANGE
            Project project = persist("Portal do Cliente", true);

            // ACT
            deleteProject.execute(project.getId());

            // ASSERT
            assertThat(project.getDeletedAt()).isNotNull();
            assertThat(project.isActive()).isFalse();
            assertThat(repository.existsById(project.getId())).isTrue();
        }

        @Test
        @DisplayName("deve tratar projeto ja excluido como inexistente")
        void shouldRejectSecondDelete() {
            // ARRANGE
            Project project = persist("Portal do Cliente", true);
            deleteProject.execute(project.getId());

            // ACT + ASSERT
            assertThatThrownBy(() -> deleteProject.execute(project.getId()))
                    .isInstanceOf(ProjectNotFoundException.class);
        }

        @Test
        @DisplayName("deve falhar para um identificador desconhecido")
        void shouldRejectUnknownId() {
            assertThatThrownBy(() -> deleteProject.execute(UUID.randomUUID()))
                    .isInstanceOf(ProjectNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("SearchProjectsUseCase")
    class Search {

        @Test
        @DisplayName("deve filtrar por trecho do nome ignorando maiusculas")
        void shouldFilterByName() {
            // ARRANGE
            persist("Portal do Cliente", true);
            persist("App Mobile", true);

            // ACT
            List<Project> found = searchProjects.execute(new SearchProjectsQuery("PORTAL", null));

            // ASSERT
            assertThat(found).extracting(Project::getName).containsExactly("Portal do Cliente");
        }

        @Test
        @DisplayName("deve devolver os projetos em ordem alfabetica")
        void shouldSortByName() {
            // ARRANGE
            persist("Portal do Cliente", true);
            persist("App Mobile", true);

            // ACT
            List<Project> found = searchProjects.execute(new SearchProjectsQuery(null, null));

            // ASSERT
            assertThat(found).extracting(Project::getName)
                    .containsExactly("App Mobile", "Portal do Cliente");
        }

        @Test
        @DisplayName("deve filtrar somente os projetos ativos quando solicitado")
        void shouldFilterByActiveFlag() {
            // ARRANGE
            persist("Portal do Cliente", true);
            persist("App Mobile", false);

            // ACT
            List<Project> found = searchProjects.execute(new SearchProjectsQuery(null, true));

            // ASSERT
            assertThat(found).extracting(Project::getName).containsExactly("Portal do Cliente");
        }

        @Test
        @DisplayName("nao deve devolver projetos excluidos")
        void shouldHideDeletedProjects() {
            // ARRANGE
            Project project = persist("Portal do Cliente", true);
            deleteProject.execute(project.getId());

            // ACT
            List<Project> found = searchProjects.execute(new SearchProjectsQuery(null, null));

            // ASSERT
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("FindProjectRoutesUseCase")
    class Routes {

        @Test
        @DisplayName("deve filtrar as rotas pelo metodo HTTP e ordenar por caminho")
        void shouldFilterAndSortRoutes() {
            // ARRANGE
            Project project = persist("Portal do Cliente", true);
            project.addRoute(Route.builder().name("Listar usuarios").httpMethod("GET").path("/users").build());
            project.addRoute(Route.builder().name("Criar usuario").httpMethod("POST").path("/users").build());
            project.addRoute(Route.builder().name("Listar contas").httpMethod("get").path("/accounts").build());

            // ACT
            List<Route> found = findProjectRoutes.execute(project.getId(), "GET");

            // ASSERT
            assertThat(found).extracting(Route::getPath).containsExactly("/accounts", "/users");
        }

        @Test
        @DisplayName("deve devolver todas as rotas quando nenhum metodo e informado")
        void shouldReturnAllRoutesWithoutFilter() {
            // ARRANGE
            Project project = persist("Portal do Cliente", true);
            project.addRoute(Route.builder().name("Listar usuarios").httpMethod("GET").path("/users").build());
            project.addRoute(Route.builder().name("Criar usuario").httpMethod("POST").path("/users").build());

            // ACT
            List<Route> found = findProjectRoutes.execute(project.getId(), null);

            // ASSERT
            assertThat(found).hasSize(2);
        }
    }
}
