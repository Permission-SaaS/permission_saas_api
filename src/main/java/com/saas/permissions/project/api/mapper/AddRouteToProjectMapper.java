package com.saas.permissions.project.api.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.api.dto.AddRouteRequest;
import com.saas.permissions.project.application.command.AddRouteToProjectCommand;

/**
 * O projeto vem do caminho e a rota do corpo; ver UpdateProjectMapper.
 */
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
