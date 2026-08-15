package com.saas.permissions.project.domain.project.exception;

import java.util.UUID;

import com.saas.permissions.shared.domain.exception.ResourceNotFoundException;

public class ProjectNotFoundException extends ResourceNotFoundException {
    public ProjectNotFoundException(UUID projectId) {
        super("Project not found: " + projectId);
    }

}
