package com.saas.permissions.project.api.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.saas.permissions.project.api.dto.UpdateProjectRequest;
import com.saas.permissions.project.application.command.UpdateProjectCommand;

/**
 * Nao implementa Mapper<I,O> porque o command reune duas origens: o
 * identificador vem do caminho e o restante do corpo. A montagem fica aqui, e
 * nao no controller, para o controller apenas repassar dados.
 */
@Component
public class UpdateProjectMapper {

    public UpdateProjectCommand map(UUID projectId, UpdateProjectRequest request) {
        return new UpdateProjectCommand(
                projectId,
                request.name(),
                request.description(),
                request.maxRoles());
    }
}
