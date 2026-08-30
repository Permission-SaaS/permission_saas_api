package com.saas.permissions.audit.domain;

import java.util.List;

/**
 * Porta da trilha de auditoria. So registra e le: um evento de auditoria nunca
 * e alterado nem removido, e o que o torna prova do que aconteceu.
 */
public interface AuditEventRepository {

    AuditEvent save(AuditEvent event);

    List<AuditEvent> findAll();
}
