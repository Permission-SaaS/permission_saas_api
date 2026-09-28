package com.saas.permissions.project.application.project;

import java.util.List;

import org.springframework.stereotype.Service;

import com.saas.permissions.project.application.project.query.SearchProjectsQuery;
import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.project.ProjectRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchProjectsUseCase {

    private final ProjectRepository projectRepository;

    public List<Project> execute(SearchProjectsQuery query) {
        return projectRepository.searchByName(query.name()).stream()
                .filter(project -> matchesActive(project, query.onlyActive()))
                .toList();
    }

    private boolean matchesActive(Project project, Boolean onlyActive) {
        if (onlyActive == null) {
            return true;
        }

        return project.isActive() == onlyActive;
    }
}
