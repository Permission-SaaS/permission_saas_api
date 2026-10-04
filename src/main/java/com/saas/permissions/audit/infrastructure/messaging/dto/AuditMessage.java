package com.saas.permissions.audit.infrastructure.messaging.dto;

import java.time.Instant;

/**
 * Envelope de toda mensagem da fila de auditoria: quem enviou, o tipo do evento,
 * quando aconteceu e os dados próprios do tipo em {@code payload}. Genérico de
 * propósito, para o audit-service poder receber eventos de outros sistemas.
 */
public record AuditMessage<T>(
        String source,
        String type,
        Instant occurredAt,
        T payload) {
}
