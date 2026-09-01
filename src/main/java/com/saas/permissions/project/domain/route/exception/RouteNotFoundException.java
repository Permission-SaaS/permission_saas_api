package com.saas.permissions.project.domain.route.exception;

import java.util.UUID;

import com.saas.permissions.shared.domain.exception.ResourceNotFoundException;

public class RouteNotFoundException extends ResourceNotFoundException {
    public RouteNotFoundException(UUID routeId) {
        super("Route not found in this project: " + routeId);
    }
}
