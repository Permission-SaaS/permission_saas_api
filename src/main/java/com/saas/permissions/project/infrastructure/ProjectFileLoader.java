package com.saas.permissions.project.infrastructure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.core.io.ClassPathResource;

import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.project.exception.ProjectNotFoundException;
import com.saas.permissions.project.domain.role.Role;
import com.saas.permissions.project.domain.route.Route;

public class ProjectFileLoader {

    private static final String PROJECTS_FILE = "data/projects.txt";
    private static final String ROLES_FILE = "data/roles.txt";
    private static final String ROUTES_FILE = "data/routes.txt";

    private static final int EXPECTED_PROJECT_FIELDS = 7;
    private static final int EXPECTED_ROLE_FIELDS = 6;
    private static final int EXPECTED_ROUTE_FIELDS = 8;

    private static final String SEPARATOR = ";";
    private static final String COMMENT_PREFIX = "#";

    public List<Project> load() {
        ClassPathResource projectResource = resource(PROJECTS_FILE);
        ClassPathResource roleResource = resource(ROLES_FILE);
        ClassPathResource routeResource = resource(ROUTES_FILE);

        Map<UUID, Project> projects = new LinkedHashMap<>();

        try (
                BufferedReader projectReader = reader(projectResource);
                BufferedReader roleReader = reader(roleResource);
                BufferedReader routeReader = reader(routeResource)) {

            eachDataLine(projectReader, PROJECTS_FILE, EXPECTED_PROJECT_FIELDS, parts -> {
                Project project = parseProject(parts);

                projects.put(project.getId(), project);
            });

            eachDataLine(roleReader, ROLES_FILE, EXPECTED_ROLE_FIELDS, parts -> {
                Role role = parseRole(parts);

                parentOf(projects, role.getProjectId()).addRole(role);
            });

            eachDataLine(routeReader, ROUTES_FILE, EXPECTED_ROUTE_FIELDS, parts -> {
                Route route = parseRoute(parts);

                parentOf(projects, route.getProjectId()).addRoute(route);
            });

            return List.copyOf(projects.values());
        } catch (IOException e) {
            throw new SeedFileException(PROJECTS_FILE, e);
        }
    }

    private ClassPathResource resource(String fileName) {
        ClassPathResource resource = new ClassPathResource(fileName);

        if (!resource.exists()) {
            throw new SeedFileException(fileName);
        }

        return resource;
    }

    private BufferedReader reader(ClassPathResource resource) {
        try {
            return new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SeedFileException(resource.getFilename(), e);
        }
    }

    private void eachDataLine(BufferedReader reader, String fileName, int expectedFields,
            Consumer<String[]> handler) {
        reader.lines().forEach(line -> {
            String content = line.strip();

            if (content.isEmpty() || content.startsWith(COMMENT_PREFIX)) {
                return;
            }

            String[] parts = content.split(SEPARATOR, -1);

            if (parts.length != expectedFields) {
                throw new SeedFileException(fileName, line, expectedFields, parts.length);
            }

            handler.accept(parts);
        });
    }

    private Project parentOf(Map<UUID, Project> projects, UUID projectId) {
        Project project = projects.get(projectId);

        if (project == null) {
            throw new ProjectNotFoundException(projectId);
        }

        return project;
    }

    private Project parseProject(String[] parts) {
        UUID id = UUID.fromString(requiredField(parts, 0));
        UUID clientId = UUID.fromString(requiredField(parts, 1));
        String name = requiredField(parts, 2);
        String description = field(parts, 3);
        String maxRolesValue = field(parts, 4);
        Integer maxRoles = maxRolesValue != null ? Integer.parseInt(maxRolesValue) : null;
        boolean isActive = Boolean.parseBoolean(requiredField(parts, 5));
        OffsetDateTime createdAt = OffsetDateTime.parse(requiredField(parts, 6));

        return Project.builder().id(id).clientId(clientId).name(name).description(description).maxRoles(maxRoles)
                .isActive(isActive).createdAt(createdAt).build();
    }

    private Role parseRole(String[] parts) {
        UUID id = UUID.fromString(requiredField(parts, 0));
        UUID projectId = UUID.fromString(requiredField(parts, 1));
        String name = requiredField(parts, 2);
        String description = field(parts, 3);
        boolean isActive = Boolean.parseBoolean(requiredField(parts, 4));
        OffsetDateTime createdAt = OffsetDateTime.parse(requiredField(parts, 5));

        return Role.builder().id(id).projectId(projectId).name(name).description(description)
                .isActive(isActive).createdAt(createdAt).build();
    }

    private Route parseRoute(String[] parts) {
        UUID id = UUID.fromString(requiredField(parts, 0));
        UUID projectId = UUID.fromString(requiredField(parts, 1));
        String name = requiredField(parts, 2);
        String httpMethod = requiredField(parts, 3);
        String path = requiredField(parts, 4);
        String description = field(parts, 5);
        boolean isActive = Boolean.parseBoolean(requiredField(parts, 6));
        OffsetDateTime createdAt = OffsetDateTime.parse(requiredField(parts, 7));

        return Route.builder().id(id).projectId(projectId).name(name).httpMethod(httpMethod).path(path)
                .description(description).isActive(isActive).createdAt(createdAt).build();
    }

    private String requiredField(String[] parts, int index) {
        String value = field(parts, index);

        if (value == null) {
            throw new SeedFileException(index, String.join(SEPARATOR, parts));
        }

        return value;
    }

    private String field(String[] parts, int index) {
        String value = parts[index].strip();

        return value.isEmpty() ? null : value;
    }
}
