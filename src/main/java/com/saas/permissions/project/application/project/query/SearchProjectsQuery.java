package com.saas.permissions.project.application.project.query;

public record SearchProjectsQuery(
        String name,
        Boolean onlyActive) {

}
