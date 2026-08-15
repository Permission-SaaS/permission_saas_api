package com.saas.permissions.project.domain.project.exception;

import com.saas.permissions.shared.domain.exception.BusinessRuleException;

public class PlanLimitExceededException extends BusinessRuleException {
    public PlanLimitExceededException(String projectName, int maxRoles) {
        super("Project '" + projectName + "' has reached the limit of " + maxRoles
                + " roles allowed by its plan");
    }
}
