package com.saas.permissions.project.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Alteracao parcial: um campo nulo significa "manter o valor atual". Por isso
 * nao ha @NotBlank aqui — string em branco e recusada pelo dominio, que
 * distingue "nao informado" de "informado vazio".
 */
public record UpdateProjectRequest(

        @Size(max = 120) String name,

        @Size(max = 500) String description,

        @Min(1) Integer maxRoles) {

}
