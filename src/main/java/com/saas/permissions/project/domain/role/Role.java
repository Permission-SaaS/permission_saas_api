package com.saas.permissions.project.domain.role;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.saas.permissions.project.domain.roleroute.RoleRoute;

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
public class Role {

    @Builder.Default
    private UUID id = UUID.randomUUID();

    private UUID projectId;

    private String name;
    private String description;

    @Builder.Default
    private List<RoleRoute> permissions = new ArrayList<>();

    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
    private OffsetDateTime updatedAt;

    public Optional<RoleRoute> activeAccessTo(UUID routeId) {
        return this.permissions.stream()
                .filter(RoleRoute::isActive)
                .filter(permission -> permission.getRouteId().equals(routeId))
                .findFirst();
    }

    public boolean hasActiveAccessTo(UUID routeId) {
        return activeAccessTo(routeId).isPresent();
    }

    public List<RoleRoute> activePermissions() {
        return this.permissions.stream().filter(RoleRoute::isActive).toList();
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", projectId=" + projectId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", permissions=" + permissions +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
