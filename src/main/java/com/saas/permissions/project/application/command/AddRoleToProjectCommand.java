package com.saas.permissions.project.application.command;

import java.util.UUID;

public record AddRoleToProjectCommand(
        UUID projectId,
        String name,
        String description) {

}
