package com.saas.permissions.project.api.project.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.api.project.dto.UpdateProjectRequest;
import com.saas.permissions.project.application.project.command.UpdateProjectCommand;

@Component
public class UpdateProjectMapper {

    public UpdateProjectCommand map(UUID projectId, UpdateProjectRequest request) {
        return new UpdateProjectCommand(
                projectId,
                request.name(),
                request.description(),
                request.maxRoles());
    }
}
