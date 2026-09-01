package com.saas.permissions.project.api.project.mapper;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.api.project.dto.CreateProjectRequest;
import com.saas.permissions.project.application.project.command.CreateProjectCommand;
import com.saas.permissions.shared.domain.Mapper;

@Component
public class CreateProjectMapper implements Mapper<CreateProjectRequest, CreateProjectCommand> {

    @Override
    public CreateProjectCommand map(CreateProjectRequest request) {
        return new CreateProjectCommand(
                request.clientId(),
                request.name(),
                request.description(),
                request.maxRoles());
    }
}
