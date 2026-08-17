package com.saas.permissions.project.infrastructure;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.saas.permissions.project.domain.project.Project;

@Component
@Profile("demo")
@Order(1)
public class ProjectDemoRunner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        List<Project> projects = new ProjectFileLoader().load();

        for (Project project : projects) {
            System.out.println("Loaded project: " + project);
        }
    }

}
