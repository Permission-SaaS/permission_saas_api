package com.saas.permissions.project.infrastructure.route.batch;

/**
 * Uma linha do CSV de importação, como veio do arquivo: sem tratamento nenhum. Quem
 * limpa e valida é o {@link RouteImportProcessor}.
 */
public record RouteCsvLine(
        String name,
        String httpMethod,
        String path,
        String description) {
}
