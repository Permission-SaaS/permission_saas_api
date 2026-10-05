package com.saas.permissions.project.domain.route;

/**
 * Resumo de uma importação: quantas linhas foram lidas, quantas viraram rota e
 * quantas foram descartadas (inválidas ou repetidas).
 */
public record RouteImportResult(
        long executionId,
        String status,
        long read,
        long imported,
        long discarded) {
}
