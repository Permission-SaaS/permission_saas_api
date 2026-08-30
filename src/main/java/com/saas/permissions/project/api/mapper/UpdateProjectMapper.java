package com.saas.permissions.project.api.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.api.dto.UpdateProjectRequest;
import com.saas.permissions.project.application.command.UpdateProjectCommand;

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
