package com.saas.permissions.project.domain.roleroute.exception;

import java.util.UUID;

import com.saas.permissions.shared.domain.exception.ResourceNotFoundException;

public class RouteAccessNotFoundException extends ResourceNotFoundException {
    public RouteAccessNotFoundException(UUID roleId, UUID routeId) {
        super("Role " + roleId + " has no active grant on route " + routeId);
    }
}
