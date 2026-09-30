package com.saas.permissions.audit.domain.exception;

public class AuditTrailUnavailableException extends RuntimeException {
    
    public AuditTrailUnavailableException (Throwable cause) {
        super("Audit service is unavailable", cause);
    }

}
