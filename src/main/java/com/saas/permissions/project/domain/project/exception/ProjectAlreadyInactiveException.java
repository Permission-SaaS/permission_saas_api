package com.saas.permissions.project.domain.project.exception;

import com.saas.permissions.shared.domain.exception.BusinessRuleException;

public class ProjectAlreadyInactiveException extends BusinessRuleException {
    public ProjectAlreadyInactiveException(String projectName) {
        super("Project '" + projectName + "' is already inactive");
    }
}
