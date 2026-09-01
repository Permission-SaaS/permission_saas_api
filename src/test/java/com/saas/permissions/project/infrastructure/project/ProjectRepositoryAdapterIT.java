package com.saas.permissions.project.infrastructure.project;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.project.ProjectRepository;
import com.saas.permissions.project.domain.role.Role;
import com.saas.permissions.project.domain.roleroute.RoleRoute;
import com.saas.permissions.project.domain.route.Route;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ProjectRepositoryAdapter (IT)")
class ProjectRepositoryAdapterIT {

    @Autowired
    private ProjectRepository projectRepository;

    private Project newProject(String name) {
        return Project.builder()
                .clientId(UUID.randomUUID())
                .name(name)
                .description("Descricao de " + name)
                .maxRoles(5)
                .build();
    }

    @Nested
    @DisplayName("relacionamento um-para-muitos")
    class OneToMany {

        @Test
        @DisplayName("deve gravar e recarregar o projeto com seus cargos e rotas")
        void shouldPersistAggregate() {
            // ARRANGE
            Project project = newProject("Portal " + UUID.randomUUID());
            project.addRole(Role.builder().name("ADMIN").description("Administrador").build());
            project.addRoute(Route.builder().name("Listar").httpMethod("GET").path("/produtos").build());

            // ACT
            projectRepository.save(project);
            Project reloaded = projectRepository.findById(project.getId()).orElseThrow();

            // ASSERT
            assertThat(reloaded.getRoles()).extracting(Role::getName).containsExactly("ADMIN");
            assertThat(reloaded.getRoutes()).extracting(Route::getPath).containsExactly("/produtos");
            assertThat(reloaded.getRoles().get(0).getProjectId()).isEqualTo(project.getId());
            assertThat(reloaded.getRoutes().get(0).getProjectId()).isEqualTo(project.getId());
        }

        @Test
        @DisplayName("deve refletir cargo adicionado a um projeto ja gravado")
        void shouldAppendChildOnUpdate() {
            // ARRANGE
            Project project = newProject("Portal " + UUID.randomUUID());
            project.addRole(Role.builder().name("ADMIN").build());
            projectRepository.save(project);

            // ACT
            Project loaded = projectRepository.findById(project.getId()).orElseThrow();
            loaded.addRole(Role.builder().name("VENDEDOR").build());
            projectRepository.save(loaded);

            // ASSERT
            Project reloaded = projectRepository.findById(project.getId()).orElseThrow();
            assertThat(reloaded.getRoles()).extracting(Role::getName)
                    .containsExactlyInAnyOrder("ADMIN", "VENDEDOR");
        }
    }

    @Nested
    @DisplayName("associacao Role x Route com historico")
    class RolePermissions {

        @Test
        @DisplayName("deve gravar e recarregar a concessao de uma rota a um cargo")
        void shouldPersistGrant() {
            // ARRANGE
            Project project = newProject("Portal " + UUID.randomUUID());
            Role role = Role.builder().name("VENDEDOR").build();
            Route route = Route.builder().name("Listar").httpMethod("GET").path("/produtos").build();
            project.addRole(role);
            project.addRoute(route);
            project.grantRouteToRole(role.getId(), route.getId());

            // ACT
            projectRepository.save(project);
            Project reloaded = projectRepository.findById(project.getId()).orElseThrow();

            // ASSERT
            Role reloadedRole = reloaded.findRole(role.getId()).orElseThrow();
            assertThat(reloadedRole.hasActiveAccessTo(route.getId())).isTrue();
            assertThat(reloaded.allows(role.getId(), route.getId())).isTrue();
        }

        @Test
        @DisplayName("deve preservar a linha revogada como historico e aceitar uma nova concessao")
        void shouldKeepRevokedGrantAsHistory() {
            // ARRANGE
            Project project = newProject("Portal " + UUID.randomUUID());
            Role role = Role.builder().name("VENDEDOR").build();
            Route route = Route.builder().name("Listar").httpMethod("GET").path("/produtos").build();
            project.addRole(role);
            project.addRoute(route);
            project.grantRouteToRole(role.getId(), route.getId());
            projectRepository.save(project);

            // ACT
            Project loaded = projectRepository.findById(project.getId()).orElseThrow();
            loaded.revokeRouteFromRole(role.getId(), route.getId());
            loaded.grantRouteToRole(role.getId(), route.getId());
            projectRepository.save(loaded);

            // ASSERT
            Role reloadedRole = projectRepository.findById(project.getId()).orElseThrow()
                    .findRole(role.getId()).orElseThrow();

            assertThat(reloadedRole.getPermissions()).hasSize(2);
            assertThat(reloadedRole.activePermissions()).hasSize(1);
            assertThat(reloadedRole.getPermissions().stream().filter(permission -> !permission.isActive()))
                    .singleElement()
                    .extracting(RoleRoute::getRevokedAt)
                    .isNotNull();
        }

        @Test
        @DisplayName("nao deve permitir acesso a uma rota sem concessao")
        void shouldDenyWithoutGrant() {
            // ARRANGE
            Project project = newProject("Portal " + UUID.randomUUID());
            Role role = Role.builder().name("VENDEDOR").build();
            Route route = Route.builder().name("Remover").httpMethod("DELETE").path("/produtos/1").build();
            project.addRole(role);
            project.addRoute(route);

            // ACT
            projectRepository.save(project);
            Project reloaded = projectRepository.findById(project.getId()).orElseThrow();

            // ASSERT
            assertThat(reloaded.allows(role.getId(), route.getId())).isFalse();
        }
    }

    @Nested
    @DisplayName("consultas derivadas")
    class DerivedQueries {

        @Test
        @DisplayName("deve filtrar por trecho do nome ignorando maiusculas")
        void shouldSearchByName() {
            // ARRANGE
            String marker = UUID.randomUUID().toString().substring(0, 8);
            projectRepository.save(newProject("Contabil " + marker));
            projectRepository.save(newProject("Estoque " + marker));

            // ACT
            List<Project> found = projectRepository.searchByName("CONTABIL " + marker);

            // ASSERT
            assertThat(found).extracting(Project::getName).containsExactly("Contabil " + marker);
        }

        @Test
        @DisplayName("nao deve devolver projetos excluidos logicamente")
        void shouldHideSoftDeleted() {
            // ARRANGE
            Project project = newProject("Excluido " + UUID.randomUUID());
            projectRepository.save(project);
            project.delete();
            projectRepository.save(project);

            // ACT
            List<Project> active = projectRepository.findAllActive();

            // ASSERT
            assertThat(active).extracting(Project::getId).doesNotContain(project.getId());
            assertThat(projectRepository.findById(project.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("remocao definitiva")
    class Purge {

        @Test
        @DisplayName("deve remover em cascata os cargos e as rotas do projeto")
        void shouldCascadeDelete() {
            // ARRANGE
            Project project = newProject("Descartavel " + UUID.randomUUID());
            project.addRole(Role.builder().name("ADMIN").build());
            project.addRoute(Route.builder().name("Listar").httpMethod("GET").path("/x").build());
            projectRepository.save(project);

            // ACT
            projectRepository.deleteById(project.getId());

            // ASSERT
            assertThat(projectRepository.existsById(project.getId())).isFalse();
        }
    }
}
