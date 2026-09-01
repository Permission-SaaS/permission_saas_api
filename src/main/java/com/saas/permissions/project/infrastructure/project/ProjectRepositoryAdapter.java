package com.saas.permissions.project.infrastructure.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.project.ProjectRepository;
import com.saas.permissions.project.domain.role.Role;
import com.saas.permissions.project.domain.roleroute.RoleRoute;
import com.saas.permissions.project.domain.route.Route;
import com.saas.permissions.project.domain.route.exception.RouteNotFoundException;
import com.saas.permissions.project.infrastructure.role.RoleJpaEntity;
import com.saas.permissions.project.infrastructure.role.RoleJpaMapper;
import com.saas.permissions.project.infrastructure.roleroute.RoleRouteJpaEntity;
import com.saas.permissions.project.infrastructure.roleroute.RoleRouteJpaMapper;
import com.saas.permissions.project.infrastructure.route.RouteJpaEntity;
import com.saas.permissions.project.infrastructure.route.RouteJpaMapper;

import lombok.RequiredArgsConstructor;

/**
 * Implementa a porta {@link ProjectRepository} sobre Spring Data JPA.
 *
 * <p>
 * A tradução entre domínio e entidade JPA fica nos mappers de cada submódulo. O
 * que é
 * responsabilidade daqui é o que só o agregado enxerga: decidir, para cada
 * filho, se ele é novo,
 * alterado ou removido, e aplicar isso na coleção que o EntityManager está
 * gerenciando.
 */
@Repository
@RequiredArgsConstructor
@Transactional
public class ProjectRepositoryAdapter implements ProjectRepository {

    private final JpaProjectRepository jpa;

    private final ProjectJpaMapper projectJpaMapper;
    private final RoleJpaMapper roleJpaMapper;
    private final RouteJpaMapper routeJpaMapper;
    private final RoleRouteJpaMapper roleRouteJpaMapper;

    /**
     * Grava em duas etapas de propósito: as concessões só entram depois que
     * projeto, cargos e rotas
     * estão gerenciados, porque {@code RoleRouteJpaEntity} referencia a rota por
     * {@code @ManyToOne}
     * sem cascata. Ver docs/ARCHITECTURE.md, ADR-007.
     */
    @Override
    public Project save(Project project) {
        ProjectJpaEntity entity = jpa.findById(project.getId())
                .map(existing -> merge(existing, project))
                .orElseGet(() -> projectJpaMapper.toJpa(project));

        ProjectJpaEntity saved = jpa.save(entity);

        mergeAllPermissions(saved, project);

        return projectJpaMapper.toDomain(jpa.save(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> findById(UUID id) {
        return jpa.findById(id).map(projectJpaMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findAll() {
        return jpa.findAll().stream().map(projectJpaMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findAllActive() {
        return jpa.findByDeletedAtIsNullOrderByCreatedAtAsc().stream()
                .map(projectJpaMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> searchByName(String name) {
        String term = name == null ? "" : name.strip();

        return jpa.findByDeletedAtIsNullAndNameContainingIgnoreCaseOrderByNameAsc(term).stream()
                .map(projectJpaMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findAllByClientId(UUID clientId) {
        return jpa.findByClientIdAndDeletedAtIsNullOrderByCreatedAtAsc(clientId).stream()
                .map(projectJpaMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return jpa.existsById(id);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }

    private ProjectJpaEntity merge(ProjectJpaEntity entity, Project project) {
        projectJpaMapper.copy(project, entity);

        mergeRoutes(entity, project.getRoutes());
        mergeRoles(entity, project.getRoles());

        return entity;
    }

    /**
     * Casa a lista do domínio com a da entidade gerenciada: apaga os que sumiram,
     * atualiza os que
     * continuam e acrescenta os novos. É preciso mexer na própria coleção que o
     * Hibernate gerencia —
     * trocá-la por outra lista faz o {@code orphanRemoval = true} falhar.
     */
    private void mergeRoles(ProjectJpaEntity entity, List<Role> roles) {
        entity.getRoles().removeIf(roleEntity -> findRole(roles, roleEntity.getId()).isEmpty());

        roles.forEach(role -> findRoleEntity(entity, role.getId())
                .ifPresentOrElse(
                        target -> roleJpaMapper.copy(role, target),
                        () -> entity.addRole(roleJpaMapper.toJpa(role))));
    }

    private void mergeRoutes(ProjectJpaEntity entity, List<Route> routes) {
        entity.getRoutes().removeIf(routeEntity -> findRoute(routes, routeEntity.getId()).isEmpty());

        routes.forEach(route -> findRouteEntity(entity, route.getId())
                .ifPresentOrElse(
                        target -> routeJpaMapper.copy(route, target),
                        () -> entity.addRoute(routeJpaMapper.toJpa(route))));
    }

    private void mergeAllPermissions(ProjectJpaEntity entity, Project project) {
        project.getRoles().forEach(role -> findRoleEntity(entity, role.getId())
                .ifPresent(target -> mergePermissions(entity, target, role)));
    }

    private void mergePermissions(ProjectJpaEntity entity, RoleJpaEntity target, Role role) {
        target.getPermissions().removeIf(
                permission -> findPermission(role, permission.getId()).isEmpty());

        role.getPermissions().forEach(permission -> findPermissionEntity(target, permission.getId())
                .ifPresentOrElse(
                        existing -> roleRouteJpaMapper.copy(permission, existing),
                        () -> target.addPermission(
                                roleRouteJpaMapper.toJpa(permission, requireRoute(entity, permission)))));
    }

    private RouteJpaEntity requireRoute(ProjectJpaEntity entity, RoleRoute permission) {
        return findRouteEntity(entity, permission.getRouteId())
                .orElseThrow(() -> new RouteNotFoundException(permission.getRouteId()));
    }

    private Optional<RoleJpaEntity> findRoleEntity(ProjectJpaEntity entity, UUID roleId) {
        return entity.getRoles().stream()
                .filter(candidate -> candidate.getId().equals(roleId))
                .findFirst();
    }

    private Optional<RouteJpaEntity> findRouteEntity(ProjectJpaEntity entity, UUID routeId) {
        return entity.getRoutes().stream()
                .filter(candidate -> candidate.getId().equals(routeId))
                .findFirst();
    }

    private Optional<RoleRouteJpaEntity> findPermissionEntity(RoleJpaEntity role, UUID permissionId) {
        return role.getPermissions().stream()
                .filter(candidate -> candidate.getId().equals(permissionId))
                .findFirst();
    }

    private Optional<Role> findRole(List<Role> roles, UUID roleId) {
        return roles.stream().filter(role -> role.getId().equals(roleId)).findFirst();
    }

    private Optional<Route> findRoute(List<Route> routes, UUID routeId) {
        return routes.stream().filter(route -> route.getId().equals(routeId)).findFirst();
    }

    private Optional<RoleRoute> findPermission(Role role, UUID permissionId) {
        return role.getPermissions().stream()
                .filter(permission -> permission.getId().equals(permissionId))
                .findFirst();
    }
}
