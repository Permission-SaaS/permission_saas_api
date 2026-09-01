package com.saas.permissions.project.infrastructure.roleroute;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.domain.roleroute.RoleRoute;
import com.saas.permissions.project.infrastructure.route.RouteJpaEntity;

/**
 * Traduz {@link RoleRoute} entre o domínio e a persistência.
 *
 * <p>
 * Não implementa {@code Mapper<I,O>} porque a tradução não é de mão única nem
 * de um argumento só:
 * {@code toJpa} precisa da {@link RouteJpaEntity} já gerenciada pelo
 * EntityManager e {@code toDomain}
 * precisa do id do cargo dono da concessão. É a mesma razão pela qual alguns
 * mappers da camada
 * {@code api} ficam fora da interface.
 */
@Component
public class RoleRouteJpaMapper {

    public RoleRouteJpaEntity toJpa(RoleRoute permission, RouteJpaEntity route) {
        return RoleRouteJpaEntity.builder()
                .id(permission.getId())
                .route(route)
                .grantedAt(permission.getGrantedAt())
                .revokedAt(permission.getRevokedAt())
                .build();
    }

    public RoleRoute toDomain(RoleRouteJpaEntity entity, UUID roleId) {
        return RoleRoute.builder()
                .id(entity.getId())
                .roleId(roleId)
                .routeId(entity.getRoute().getId())
                .grantedAt(entity.getGrantedAt())
                .revokedAt(entity.getRevokedAt())
                .build();
    }

    public void copy(RoleRoute permission, RoleRouteJpaEntity target) {
        target.setRevokedAt(permission.getRevokedAt());
    }
}
