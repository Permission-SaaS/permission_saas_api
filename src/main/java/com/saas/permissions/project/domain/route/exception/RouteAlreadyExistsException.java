package com.saas.permissions.project.domain.route.exception;

import com.saas.permissions.shared.domain.exception.BusinessRuleException;

public class RouteAlreadyExistsException extends BusinessRuleException {
    public RouteAlreadyExistsException(String httpMethod, String path) {
        super("Route '" + httpMethod + " " + path + "' already exists in this project");
    }
}
