package com.saas.permissions.project.application.project.command;

import java.util.UUID;

public record CreateProjectCommand(
        UUID clientId,
        String name,
        String description,
        Integer maxRoles) {
}
