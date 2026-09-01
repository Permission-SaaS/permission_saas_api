package com.saas.permissions.project.application.role.command;

import java.util.UUID;

public record AddRoleToProjectCommand(
        UUID projectId,
        String name,
        String description) {

}
