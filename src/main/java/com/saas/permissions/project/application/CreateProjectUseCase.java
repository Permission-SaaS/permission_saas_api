package com.saas.permissions.project.application;

import org.springframework.stereotype.Service;

import com.saas.permissions.project.application.command.CreateProjectCommand;
import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.project.ProjectRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateProjectUseCase {

    private final ProjectRepository projectRepository;

    public Project execute(CreateProjectCommand command) {
        Project project = Project.builder()
                .clientId(command.clientId())
                .name(command.name())
                .description(command.description())
                .maxRoles(command.maxRoles())
                .build();

        return projectRepository.save(project);
    }
}
