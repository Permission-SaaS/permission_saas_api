package com.saas.permissions.audit.infrastructure;

public class UnsupportedAuditEventException extends RuntimeException {

    public UnsupportedAuditEventException(Class<?> type) {
        super("Tipo de evento de auditoria nao mapeado para persistencia: " + type.getSimpleName());
    }
}
