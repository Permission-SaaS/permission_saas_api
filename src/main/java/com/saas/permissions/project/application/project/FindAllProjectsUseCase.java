package com.saas.permissions.project.application.project;

import java.util.List;

import org.springframework.stereotype.Service;

import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.project.ProjectRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindAllProjectsUseCase {

    private final ProjectRepository projectRepository;

    public List<Project> execute() {
        return projectRepository.findAllActive();
    }
}
