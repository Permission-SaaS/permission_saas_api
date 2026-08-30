package com.saas.permissions.permission.domain.event;

import java.time.OffsetDateTime;

/**
 * Publicado depois de cada validacao de permissao. Nao carrega a ApiKey: o
 * evento atravessa a fronteira do modulo e a chave e credencial, nao dado de
 * auditoria.
 */
public record PermissionValidatedEvent(
        String role,
        String route,
        boolean granted,
        String reason,
        double durationMs,
        OffsetDateTime occurredAt) {

}
