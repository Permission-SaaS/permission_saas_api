package com.saas.permissions.audit.domain.exception;

import com.saas.permissions.shared.domain.exception.ServiceUnavailableException;

public class AuditTrailUnavailableException extends ServiceUnavailableException {

    public AuditTrailUnavailableException(Throwable cause) {
        super("Audit service is unavailable", cause);
    }

}
