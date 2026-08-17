package com.saas.permissions.project.application;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.project.ProjectRepository;
import com.saas.permissions.project.domain.project.exception.ProjectNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindProjectByIdUseCase {

    private final ProjectRepository projectRepository;

    public Project execute(UUID projectId) {
        return projectRepository.findById(projectId)
                .filter(project -> project.getDeletedAt() == null)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }
}
