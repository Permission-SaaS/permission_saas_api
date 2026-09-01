package com.saas.permissions.project.domain.roleroute.exception;

import java.util.UUID;

import com.saas.permissions.shared.domain.exception.BusinessRuleException;

public class RouteAccessAlreadyRevokedException extends BusinessRuleException {
    public RouteAccessAlreadyRevokedException(UUID roleId, UUID routeId) {
        super("Access of role " + roleId + " to route " + routeId + " was already revoked");
    }
}
