package com.saas.permissions.project.api.role.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.api.role.dto.AddRoleRequest;
import com.saas.permissions.project.application.role.command.AddRoleToProjectCommand;

@Component
public class AddRoleToProjectMapper {

    public AddRoleToProjectCommand map(UUID projectId, AddRoleRequest request) {
        return new AddRoleToProjectCommand(
                projectId,
                request.name(),
                request.description());
    }
}
