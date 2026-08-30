package com.saas.permissions.project.application.command;

public record SearchProjectsQuery(
                String name,
                Boolean onlyActive) {

}
