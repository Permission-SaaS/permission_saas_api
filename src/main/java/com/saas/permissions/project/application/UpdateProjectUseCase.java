package com.saas.permissions.project.application;

import org.springframework.stereotype.Service;

import com.saas.permissions.project.application.command.UpdateProjectCommand;
import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.project.ProjectRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateProjectUseCase {

    private final FindProjectByIdUseCase findProjectByIdUseCase;

    private final ProjectRepository projectRepository;

    public Project execute(UpdateProjectCommand command) {

        Project project = findProjectByIdUseCase.execute(command.projectId());

        project.update(command.name(), command.description(), command.maxRoles());

        projectRepository.save(project);

        return project;
    }
}
