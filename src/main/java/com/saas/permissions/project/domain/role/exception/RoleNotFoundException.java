package com.saas.permissions.project.domain.role.exception;

import java.util.UUID;

import com.saas.permissions.shared.domain.exception.ResourceNotFoundException;

public class RoleNotFoundException extends ResourceNotFoundException {
    public RoleNotFoundException(UUID roleId) {
        super("Role not found in this project: " + roleId);
    }
}
