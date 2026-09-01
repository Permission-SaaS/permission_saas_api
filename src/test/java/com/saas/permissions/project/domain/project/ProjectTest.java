package com.saas.permissions.project.domain.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.saas.permissions.project.domain.project.exception.InvalidProjectDataException;
import com.saas.permissions.project.domain.project.exception.PlanLimitExceededException;
import com.saas.permissions.project.domain.project.exception.ProjectAlreadyDeletedException;
import com.saas.permissions.project.domain.role.Role;

@DisplayName("Project")
class ProjectTest {

    private Project projectWithRoles(int maxRoles, String... roleNames) {
        Project project = Project.builder()
                .name("Portal do Cliente")
                .description("Descricao original")
                .maxRoles(maxRoles)
                .build();

        for (String roleName : roleNames) {
            project.addRole(Role.builder().name(roleName).build());
        }

        return project;
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("deve alterar nome, descricao e limite de cargos")
        void shouldUpdateAllFields() {
            // ARRANGE
            Project project = projectWithRoles(5, "Admin");

            // ACT
            project.update("Portal Interno", "Nova descricao", 10);

            // ASSERT
            assertThat(project.getName()).isEqualTo("Portal Interno");
            assertThat(project.getDescription()).isEqualTo("Nova descricao");
            assertThat(project.getMaxRoles()).isEqualTo(10);
            assertThat(project.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("deve manter o valor atual quando o campo vem nulo")
        void shouldKeepCurrentValueWhenFieldIsNull() {
            // ARRANGE
            Project project = projectWithRoles(5);

            // ACT
            project.update(null, null, 8);

            // ASSERT
            assertThat(project.getName()).isEqualTo("Portal do Cliente");
            assertThat(project.getDescription()).isEqualTo("Descricao original");
            assertThat(project.getMaxRoles()).isEqualTo(8);
        }

        @Test
        @DisplayName("deve rejeitar nome em branco")
        void shouldRejectBlankName() {
            Project project = projectWithRoles(5);

            assertThatThrownBy(() -> project.update("   ", null, null))
                    .isInstanceOf(InvalidProjectDataException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("deve rejeitar descricao em branco")
        void shouldRejectBlankDescription() {
            Project project = projectWithRoles(5);

            assertThatThrownBy(() -> project.update(null, "", null))
                    .isInstanceOf(InvalidProjectDataException.class)
                    .hasMessageContaining("description");
        }

        @Test
        @DisplayName("deve rejeitar limite de cargos zero ou negativo")
        void shouldRejectNonPositiveMaxRoles() {
            Project project = projectWithRoles(5);

            assertThatThrownBy(() -> project.update(null, null, 0))
                    .isInstanceOf(InvalidProjectDataException.class)
                    .hasMessageContaining("maxRoles");

            assertThatThrownBy(() -> project.update(null, null, -5))
                    .isInstanceOf(InvalidProjectDataException.class)
                    .hasMessageContaining("maxRoles");
        }

        @Test
        @DisplayName("deve rejeitar limite menor que os cargos ja cadastrados")
        void shouldRejectMaxRolesBelowCurrentRoles() {
            // ARRANGE
            Project project = projectWithRoles(5, "Admin", "Editor", "Leitor");

            // ACT + ASSERT
            assertThatThrownBy(() -> project.update(null, null, 2))
                    .isInstanceOf(PlanLimitExceededException.class);
        }

        @Test
        @DisplayName("deve aceitar limite igual aos cargos ja cadastrados")
        void shouldAcceptMaxRolesEqualToCurrentRoles() {
            Project project = projectWithRoles(5, "Admin", "Editor");

            project.update(null, null, 2);

            assertThat(project.getMaxRoles()).isEqualTo(2);
        }

        @Test
        @DisplayName("nao deve alterar campo algum quando uma validacao falha")
        void shouldNotMutateAggregateWhenValidationFails() {
            // ARRANGE
            Project project = projectWithRoles(5, "Admin", "Editor", "Leitor");
            var updatedAtBefore = project.getUpdatedAt();

            // ACT
            assertThatThrownBy(() -> project.update("Portal Interno", "Nova descricao", 2))
                    .isInstanceOf(PlanLimitExceededException.class);

            // ASSERT
            assertThat(project.getName()).isEqualTo("Portal do Cliente");
            assertThat(project.getDescription()).isEqualTo("Descricao original");
            assertThat(project.getMaxRoles()).isEqualTo(5);
            assertThat(project.getUpdatedAt()).isEqualTo(updatedAtBefore);
        }

        @Test
        @DisplayName("deve rejeitar alteracao em projeto excluido")
        void shouldRejectUpdateOnDeletedProject() {
            // ARRANGE
            Project project = projectWithRoles(5);
            project.delete();

            // ACT + ASSERT
            assertThatThrownBy(() -> project.update("Portal Interno", null, null))
                    .isInstanceOf(ProjectAlreadyDeletedException.class);
        }
    }
}
