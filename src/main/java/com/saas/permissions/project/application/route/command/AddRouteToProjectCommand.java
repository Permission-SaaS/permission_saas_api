package com.saas.permissions.project.application.route.command;

import java.util.UUID;

public record AddRouteToProjectCommand(
                UUID projectId,
                String name,
                String path,
                String httpMethod,
                String description) {

}
