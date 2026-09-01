package com.saas.permissions.project.application.project;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.project.ProjectRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteProjectUseCase {

    private final FindProjectByIdUseCase findProjectByIdUseCase;

    private final ProjectRepository projectRepository;

    public void execute(UUID projectId) {

        Project project = findProjectByIdUseCase.execute(projectId);

        project.delete();

        projectRepository.save(project);
    }
}
