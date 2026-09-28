package com.saas.permissions.project.infrastructure.project;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.saas.permissions.project.application.project.FindAllProjectsUseCase;
import com.saas.permissions.project.application.route.FindProjectRoutesUseCase;
import com.saas.permissions.project.application.project.SearchProjectsUseCase;
import com.saas.permissions.project.application.project.query.SearchProjectsQuery;
import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.route.Route;

import lombok.RequiredArgsConstructor;

@Component
@Profile("demo")
@Order(1)
@RequiredArgsConstructor
public class ProjectDemoRunner implements CommandLineRunner {

    private final FindAllProjectsUseCase findAllProjectsUseCase;

    private final SearchProjectsUseCase searchProjectsUseCase;

    private final FindProjectRoutesUseCase findProjectRoutesUseCase;

    @Override
    public void run(String... args) throws Exception {
        List<Project> projects = this.findAllProjectsUseCase.execute();

        for (Project project : projects) {
            System.out.println("Loaded project: " + project);
        }

        String summary = projects.stream()
                .map(project -> project.getName() + " (" + project.getRoles().size() + " cargos, "
                        + project.getRoutes().size() + " rotas)")
                .reduce((left, right) -> left + " | " + right)
                .orElse("nenhum projeto carregado");

        System.out.println("\nResumo: " + summary);

        List<Project> found = this.searchProjectsUseCase.execute(new SearchProjectsQuery("portal", true));

        System.out.println("\nBusca por 'portal' (somente ativos): " + found.size() + " projeto(s)");
        found.forEach(project -> System.out.println("  - " + project.getName()));

        if (!projects.isEmpty()) {
            Project first = projects.get(0);
            List<Route> getRoutes = this.findProjectRoutesUseCase.execute(first.getId(), "GET");

            System.out.println("\nRotas GET de " + first.getName() + ": " + getRoutes.size());
            getRoutes.forEach(route -> System.out.println("  - " + route.getHttpMethod() + " " + route.getPath()));
        }
    }

}
