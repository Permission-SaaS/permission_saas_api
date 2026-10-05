package com.saas.permissions.project.domain.route;

import java.io.InputStream;
import java.util.UUID;

/**
 * Importa em lote as rotas de um CSV para um projeto. Quem implementa é o job do
 * Spring Batch na infraestrutura.
 */
public interface RouteImporter {

    RouteImportResult importRoutes(UUID projectId, InputStream csv);
}
