package com.saas.permissions.project.domain.role.exception;

import com.saas.permissions.shared.domain.exception.BusinessRuleException;

public class RoleAlreadyExistsException extends BusinessRuleException {
    public RoleAlreadyExistsException(String roleName) {
        super("Role '" + roleName + "' already exists in this project");
    }
}
