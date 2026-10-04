package com.saas.permissions.audit.domain;

import com.saas.permissions.audit.domain.exception.AuditEventNotPublishedException;

/**
 * Envia um evento para a trilha de auditoria sem esperar que ela o grave. Quem
 * grava é o audit-service, quando consumir a mensagem.
 */
public interface AuditEventPublisher {

    /**
     * @throws AuditEventNotPublishedException se o evento não puder ser entregue ao broker
     */
    void publish(PermissionCheckEvent event);
}
