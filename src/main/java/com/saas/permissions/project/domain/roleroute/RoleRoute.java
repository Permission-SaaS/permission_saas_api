package com.saas.permissions.project.domain.roleroute;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.saas.permissions.project.domain.roleroute.exception.RouteAccessAlreadyRevokedException;

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
public class RoleRoute {

    @Builder.Default
    private UUID id = UUID.randomUUID();

    private UUID roleId;
    private UUID routeId;

    @Builder.Default
    private OffsetDateTime grantedAt = OffsetDateTime.now();

    private OffsetDateTime revokedAt;

    public boolean isActive() {
        return this.revokedAt == null;
    }

    public void revoke() {
        if (this.revokedAt != null) {
            throw new RouteAccessAlreadyRevokedException(this.roleId, this.routeId);
        }

        this.revokedAt = OffsetDateTime.now();
    }

    @Override
    public String toString() {
        return "RoleRoute{" +
                "id=" + id +
                ", roleId=" + roleId +
                ", routeId=" + routeId +
                ", grantedAt=" + grantedAt +
                ", revokedAt=" + revokedAt +
                ", active=" + isActive() +
                '}';
    }
}
