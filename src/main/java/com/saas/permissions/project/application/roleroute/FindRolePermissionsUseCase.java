package com.saas.permissions.project.application.roleroute;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.saas.permissions.project.application.project.FindProjectByIdUseCase;
import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.role.Role;
import com.saas.permissions.project.domain.role.exception.RoleNotFoundException;
import com.saas.permissions.project.domain.roleroute.RoleRoute;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindRolePermissionsUseCase {

    private final FindProjectByIdUseCase findProjectByIdUseCase;

    public List<RoleRoute> execute(UUID projectId, UUID roleId, boolean includeRevoked) {
        Project project = findProjectByIdUseCase.execute(projectId);

        Role role = project.findRole(roleId).orElseThrow(() -> new RoleNotFoundException(roleId));

        return role.getPermissions().stream()
                .filter(permission -> includeRevoked || permission.isActive())
                .sorted(Comparator.comparing(RoleRoute::getGrantedAt).reversed())
                .toList();
    }
}
