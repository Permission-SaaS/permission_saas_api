package com.saas.permissions.project.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.saas.permissions.project.domain.project.Project;
import com.saas.permissions.project.domain.project.ProjectRepository;

import jakarta.annotation.PostConstruct;

@Repository
public class InMemoryProjectRepository implements ProjectRepository {
    private final Map<UUID, Project> projects = new ConcurrentHashMap<>();

    @Override
    public Project save(Project project) {
        ensureProject(project);

        projects.put(project.getId(), project);
        return project;
    }

    @Override
    public Optional<Project> findById(UUID id) {
        ensureId(id);

        return Optional.ofNullable(projects.get(id));
    }

    @Override
    public boolean existsById(UUID id) {
        ensureId(id);

        return projects.containsKey(id);
    }

    @Override
    public List<Project> findAll() {
        return List.copyOf(projects.values());
    }

    @PostConstruct
    void seedFromFiles() {
        new ProjectFileLoader().load().forEach(this::save);
    }

    private void ensureProject(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null");
        }

        ensureId(project.getId());
    }

    private void ensureId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Project ID cannot be null");
        }
    }
}
