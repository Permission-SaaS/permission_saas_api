package com.saas.permissions.project.application.project;

public enum RouteAccessResult {

    GRANTED,
    PROJECT_NOT_FOUND,
    ROUTE_NOT_FOUND,
    ROUTE_INACTIVE,
    ROLE_NOT_FOUND,
    ROLE_INACTIVE,
    ROLE_HAS_NO_ACCESS_TO_ROUTE
}
