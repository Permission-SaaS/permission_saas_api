package com.saas.permissions.project.domain.roleroute.exception;

import java.util.UUID;

import com.saas.permissions.shared.domain.exception.BusinessRuleException;

public class RouteAccessAlreadyGrantedException extends BusinessRuleException {
    public RouteAccessAlreadyGrantedException(UUID roleId, UUID routeId) {
        super("Role " + roleId + " already has an active grant on route " + routeId);
    }
}
