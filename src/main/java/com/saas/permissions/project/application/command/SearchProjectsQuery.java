package com.saas.permissions.project.application.command;

/**
 * Filtros opcionais da busca de projetos. Um campo nulo significa "nao filtrar
 * por este criterio", o que permite reaproveitar a mesma consulta na API.
 */
public record SearchProjectsQuery(
                String name,
                Boolean onlyActive) {

}
