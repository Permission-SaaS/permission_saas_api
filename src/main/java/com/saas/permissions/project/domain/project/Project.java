package com.saas.permissions.project.domain.project;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.saas.permissions.project.domain.project.exception.PlanLimitExceededException;
import com.saas.permissions.project.domain.project.exception.ProjectAlreadyActiveException;
import com.saas.permissions.project.domain.project.exception.ProjectAlreadyDeletedException;
import com.saas.permissions.project.domain.project.exception.ProjectAlreadyInactiveException;
import com.saas.permissions.project.domain.role.Role;
import com.saas.permissions.project.domain.role.exception.RoleAlreadyExistsException;
import com.saas.permissions.project.domain.route.Route;
import com.saas.permissions.project.domain.route.exception.RouteAlreadyExistsException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Project {

    @Builder.Default
    private UUID id = UUID.randomUUID();

    private UUID clientId;

    private String name;
    private String description;

    private Integer maxRoles;

    @Builder.Default
    private List<Route> routes = new ArrayList<>();

    @Builder.Default
    private List<Role> roles = new ArrayList<>();

    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;

    public void addRoute(Route route) {
        ensureNotDeleted();

        boolean duplicated = this.routes.stream()
                .anyMatch(foundRoute -> foundRoute.getPath().equalsIgnoreCase(route.getPath())
                        && foundRoute.getHttpMethod().equalsIgnoreCase(route.getHttpMethod()));

        if (duplicated) {
            throw new RouteAlreadyExistsException(route.getHttpMethod(), route.getPath());
        }

        this.routes.add(route);
        route.setProjectId(this.id);

        this.updatedAt = OffsetDateTime.now();
    }

    public void addRole(Role role) {
        ensureNotDeleted();

        if (this.maxRoles != null && this.roles.size() >= this.maxRoles) {
            throw new PlanLimitExceededException(this.name, this.maxRoles);
        }

        if (this.roles.stream()
                .anyMatch(foundRole -> foundRole.getName().equalsIgnoreCase(role.getName()))) {
            throw new RoleAlreadyExistsException(role.getName());
        }

        this.roles.add(role);
        role.setProjectId(this.id);

        this.updatedAt = OffsetDateTime.now();
    }

    public void deactivate() {
        ensureNotDeleted();

        if (!this.isActive) {
            throw new ProjectAlreadyInactiveException(this.name);
        }

        this.isActive = false;

        this.updatedAt = OffsetDateTime.now();
    }

    public void activate() {
        ensureNotDeleted();

        if (this.isActive) {
            throw new ProjectAlreadyActiveException(this.name);
        }

        this.isActive = true;

        this.updatedAt = OffsetDateTime.now();
    }

    public void delete() {
        ensureNotDeleted();

        OffsetDateTime now = OffsetDateTime.now();
        this.isActive = false;
        this.updatedAt = now;
        this.deletedAt = now;
    }

    private void ensureNotDeleted() {
        if (this.deletedAt != null) {
            throw new ProjectAlreadyDeletedException(this.name);
        }
    }

    @Override
    public String toString() {
        return "Project{" +
                "id=" + id +
                ", clientId=" + clientId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", maxRoles=" + maxRoles +
                ", routes=" + routes +
                ", roles=" + roles +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", deletedAt=" + deletedAt +
                '}';
    }
}