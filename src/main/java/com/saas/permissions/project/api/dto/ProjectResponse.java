package com.saas.permissions.project.api.dto;

import java.util.List;
import java.util.UUID;

/**
 * Resposta do projeto com os filhos embutidos. O relacionamento 1-N e
 * serializado apenas do pai para os filhos: Role e Route expoem o pai como
 * projectId (UUID), nao como objeto, o que elimina a referencia circular sem
 * depender de @JsonIgnore.
 */
public record ProjectResponse(
        UUID id,
        UUID clientId,
        String name,
        String description,
        Integer maxRoles,
        boolean active,
        String createdAt,
        String updatedAt,
        List<RoleResponse> roles,
        List<RouteResponse> routes) {

}
