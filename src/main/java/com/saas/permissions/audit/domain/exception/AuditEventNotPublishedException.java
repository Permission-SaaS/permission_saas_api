package com.saas.permissions.audit.domain.exception;

public class AuditEventNotPublishedException extends RuntimeException {

    public AuditEventNotPublishedException(Throwable cause) {
        super("Audit event could not be published", cause);
    }

}
