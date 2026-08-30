package com.saas.permissions.project.application;

import org.springframework.stereotype.Service;

import com.saas.permissions.project.application.command.AddRoleToProjectCommand;
import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.project.ProjectRepository;
import com.saas.permissions.project.domain.role.Role;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AddRoleToProjectUseCase {

    private final FindProjectByIdUseCase findProjectByIdUseCase;
    private final ProjectRepository projectRepository;

    public Role execute(AddRoleToProjectCommand command) {
        Project project = findProjectByIdUseCase.execute(command.projectId());

        Role role = Role.builder()
                .name(command.name())
                .description(command.description())
                .build();

        project.addRole(role);

        projectRepository.save(project);

        return role;
    }

}
