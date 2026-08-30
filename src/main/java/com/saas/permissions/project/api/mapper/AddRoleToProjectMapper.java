package com.saas.permissions.project.api.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.api.dto.AddRoleRequest;
import com.saas.permissions.project.application.command.AddRoleToProjectCommand;

@Component
public class AddRoleToProjectMapper {

    public AddRoleToProjectCommand map(UUID projectId, AddRoleRequest request) {
        return new AddRoleToProjectCommand(
                projectId,
                request.name(),
                request.description());
    }
}
