package com.saas.permissions.project.application.project;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.saas.permissions.project.domain.project.ProjectRepository;
import com.saas.permissions.project.domain.project.exception.ProjectNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PurgeProjectUseCase {

    private final ProjectRepository projectRepository;

    public void execute(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ProjectNotFoundException(projectId);
        }

        projectRepository.deleteById(projectId);
    }
}
