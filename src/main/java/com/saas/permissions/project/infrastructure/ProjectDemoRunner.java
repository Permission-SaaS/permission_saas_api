package com.saas.permissions.project.infrastructure;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.saas.permissions.project.application.FindAllProjectsUseCase;
import com.saas.permissions.project.domain.project.Project;

import lombok.RequiredArgsConstructor;

@Component
@Profile("demo")
@Order(1)
@RequiredArgsConstructor
public class ProjectDemoRunner implements CommandLineRunner {

    private final FindAllProjectsUseCase findAllProjectsUseCase;

    @Override
    public void run(String... args) throws Exception {
        List<Project> projects = this.findAllProjectsUseCase.execute();

        for (Project project : projects) {
            System.out.println("Loaded project: " + project);
        }
    }

}
