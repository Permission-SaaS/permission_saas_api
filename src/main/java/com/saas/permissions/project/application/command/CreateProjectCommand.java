package com.saas.permissions.project.application.command;

import java.util.UUID;

public record CreateProjectCommand(
        UUID clientId,
        String name,
        String description,
        Integer maxRoles) {
}
