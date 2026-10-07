package com.saas.permissions.project.application.project.command;

import java.util.UUID;

public record UpdateProjectCommand(
        UUID projectId,
        String name,
        String description,
        Integer maxRoles) {

}
