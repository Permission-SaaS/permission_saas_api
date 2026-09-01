package com.saas.permissions.project.infrastructure.project;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.role.Role;
import com.saas.permissions.project.domain.route.Route;
import com.saas.permissions.project.infrastructure.role.RoleJpaMapper;
import com.saas.permissions.project.infrastructure.route.RouteJpaMapper;

import lombok.RequiredArgsConstructor;

/**
 * Traduz o agregado inteiro delegando cada filho ao mapper do seu próprio
 * submódulo. A decisão de
 * quais filhos são novos, alterados ou removidos não é daqui: é do
 * {@link ProjectRepositoryAdapter}, que enxerga a entidade gerenciada pelo
 * EntityManager.
 */
@Component
@RequiredArgsConstructor
public class ProjectJpaMapper {

    private final RoleJpaMapper roleJpaMapper;
    private final RouteJpaMapper routeJpaMapper;

    public ProjectJpaEntity toJpa(Project project) {
        ProjectJpaEntity entity = ProjectJpaEntity.builder()
                .id(project.getId())
                .clientId(project.getClientId())
                .name(project.getName())
                .description(project.getDescription())
                .maxRoles(project.getMaxRoles())
                .isActive(project.isActive())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .deletedAt(project.getDeletedAt())
                .build();

        project.getRoutes().forEach(route -> entity.addRoute(routeJpaMapper.toJpa(route)));
        project.getRoles().forEach(role -> entity.addRole(roleJpaMapper.toJpa(role)));

        return entity;
    }

    public Project toDomain(ProjectJpaEntity entity) {
        List<Role> roles = new ArrayList<>(entity.getRoles().stream()
                .map(role -> roleJpaMapper.toDomain(role, entity.getId()))
                .toList());

        List<Route> routes = new ArrayList<>(entity.getRoutes().stream()
                .map(route -> routeJpaMapper.toDomain(route, entity.getId()))
                .toList());

        return Project.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .name(entity.getName())
                .description(entity.getDescription())
                .maxRoles(entity.getMaxRoles())
                .roles(roles)
                .routes(routes)
                .isActive(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public void copy(Project project, ProjectJpaEntity target) {
        target.setClientId(project.getClientId());
        target.setName(project.getName());
        target.setDescription(project.getDescription());
        target.setMaxRoles(project.getMaxRoles());
        target.setActive(project.isActive());
        target.setUpdatedAt(project.getUpdatedAt());
        target.setDeletedAt(project.getDeletedAt());
    }
}
