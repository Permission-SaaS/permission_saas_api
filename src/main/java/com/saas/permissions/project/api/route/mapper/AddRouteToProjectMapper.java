package com.saas.permissions.project.api.route.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.api.route.dto.AddRouteRequest;
import com.saas.permissions.project.application.route.command.AddRouteToProjectCommand;

@Component
public class AddRouteToProjectMapper {

    public AddRouteToProjectCommand map(UUID projectId, AddRouteRequest request) {
        return new AddRouteToProjectCommand(
                projectId,
                request.name(),
                request.path(),
                request.httpMethod(),
                request.description());
    }
}
