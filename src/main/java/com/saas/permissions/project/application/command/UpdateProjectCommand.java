package com.saas.permissions.project.application.command;

import java.util.UUID;

public record UpdateProjectCommand(
                UUID projectId,
                String name,
                String description,
                Integer maxRoles) {

}
