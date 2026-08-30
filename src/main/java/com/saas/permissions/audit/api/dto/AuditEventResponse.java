package com.saas.permissions.audit.api.dto;

import java.util.UUID;

/**
 * Representacao unica para toda a hierarquia de AuditEvent. O que distingue as
 * subclasses aparece em type() e description, ambos polimorficos — a API expoe
 * a heranca sem precisar de um DTO por subclasse.
 */
public record AuditEventResponse(
        UUID id,
        String type,
        UUID projectId,
        String occurredAt,
        String description) {

}
