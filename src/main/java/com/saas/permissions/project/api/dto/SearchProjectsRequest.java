package com.saas.permissions.project.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Filtros da listagem, vindos da query string. Ambos opcionais: nulo significa
 * "nao filtrar por este criterio".
 */
public record SearchProjectsRequest(

        @Size(max = 120) String name,

        Boolean onlyActive) {

}
