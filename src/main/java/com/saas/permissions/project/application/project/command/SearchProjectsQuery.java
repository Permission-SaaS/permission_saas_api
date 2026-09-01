package com.saas.permissions.project.application.project.command;

public record SearchProjectsQuery(
                String name,
                Boolean onlyActive) {

}
