package com.saas.permissions.project.domain.project.exception;

import com.saas.permissions.shared.domain.exception.BusinessRuleException;

public class ProjectAlreadyDeletedException extends BusinessRuleException {
    public ProjectAlreadyDeletedException(String projectName) {
        super("Project '" + projectName + "' has already been deleted");
    }
}
