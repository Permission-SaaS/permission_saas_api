package com.saas.permissions.project.application.route;

import com.saas.permissions.project.application.project.FindProjectByIdUseCase;

import org.springframework.stereotype.Service;

import com.saas.permissions.project.application.route.command.AddRouteToProjectCommand;
import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.project.ProjectRepository;
import com.saas.permissions.project.domain.route.Route;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AddRouteToProjectUseCase {

    private final ProjectRepository projectRepository;
    private final FindProjectByIdUseCase findProjectByIdUseCase;

    public Route execute(AddRouteToProjectCommand command) {
        Project project = findProjectByIdUseCase.execute(command.projectId());

        Route route = Route.builder()
                .name(command.name())
                .path(command.path())
                .httpMethod(command.httpMethod())
                .description(command.description())
                .build();

        project.addRoute(route);

        projectRepository.save(project);

        return route;
    }
}
