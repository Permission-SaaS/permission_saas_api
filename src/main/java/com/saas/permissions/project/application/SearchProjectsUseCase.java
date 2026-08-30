package com.saas.permissions.project.application;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.saas.permissions.project.application.command.SearchProjectsQuery;
import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.project.ProjectRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchProjectsUseCase {

    private final ProjectRepository projectRepository;

    public List<Project> execute(SearchProjectsQuery query) {
        return projectRepository.findAll().stream()
                .filter(project -> project.getDeletedAt() == null)
                .filter(project -> matchesName(project, query.name()))
                .filter(project -> matchesActive(project, query.onlyActive()))
                .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private boolean matchesName(Project project, String name) {
        if (name == null || name.isBlank()) {
            return true;
        }

        return project.getName().toLowerCase().contains(name.toLowerCase());
    }

    private boolean matchesActive(Project project, Boolean onlyActive) {
        if (onlyActive == null) {
            return true;
        }

        return project.isActive() == onlyActive;
    }
}
