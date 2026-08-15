package com.saas.permissions.project.domain.project.exception;

import com.saas.permissions.shared.domain.exception.BusinessRuleException;

public class ProjectAlreadyActiveException extends BusinessRuleException {
    public ProjectAlreadyActiveException(String projectName) {
        super("Project '" + projectName + "' is already active");
    }
}
