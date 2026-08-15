package com.saas.permissions.project.domain.role;

import java.time.OffsetDateTime;
import java.util.UUID;

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
    private boolean isActive = true;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
    private OffsetDateTime updatedAt;

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", projectId=" + projectId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
