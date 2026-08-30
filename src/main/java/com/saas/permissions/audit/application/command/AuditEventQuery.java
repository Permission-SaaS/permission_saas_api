package com.saas.permissions.audit.application.command;

import java.util.UUID;

/**
 * Filtros opcionais da consulta a trilha de auditoria. Nulo significa "nao
 * filtrar por este criterio".
 */
public record AuditEventQuery(
        String type,
        UUID projectId,
        Boolean onlyDenied) {

}
