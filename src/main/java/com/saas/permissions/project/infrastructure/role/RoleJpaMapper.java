package com.saas.permissions.project.infrastructure.role;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.domain.role.Role;
import com.saas.permissions.project.domain.roleroute.RoleRoute;
import com.saas.permissions.project.infrastructure.roleroute.RoleRouteJpaMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoleJpaMapper {

    private final RoleRouteJpaMapper roleRouteJpaMapper;

    /**
     * Monta o cargo sem as concessões de propósito: uma {@code RoleRouteJpaEntity}
     * referencia a rota
     * por {@code @ManyToOne} sem cascata, então ela só pode ser criada quando a
     * rota já estiver
     * gerenciada. Quem cuida disso é o {@code ProjectRepositoryAdapter} — ver
     * ADR-007.
     */
    public RoleJpaEntity toJpa(Role role) {
        return RoleJpaEntity.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .isActive(role.isActive())
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }

    public Role toDomain(RoleJpaEntity entity, UUID projectId) {
        List<RoleRoute> permissions = new ArrayList<>(entity.getPermissions().stream()
                .map(permission -> roleRouteJpaMapper.toDomain(permission, entity.getId()))
                .toList());

        return Role.builder()
                .id(entity.getId())
                .projectId(projectId)
                .name(entity.getName())
                .description(entity.getDescription())
                .permissions(permissions)
                .isActive(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void copy(Role role, RoleJpaEntity target) {
        target.setName(role.getName());
        target.setDescription(role.getDescription());
        target.setActive(role.isActive());
        target.setUpdatedAt(role.getUpdatedAt());
    }
}
