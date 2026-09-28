package com.saas.permissions.audit.domain.exception;

import java.time.OffsetDateTime;

import com.saas.permissions.shared.domain.exception.InvalidDataException;

public class InvalidAuditPeriodException extends InvalidDataException {

    public InvalidAuditPeriodException(OffsetDateTime from, OffsetDateTime to) {
        super("Invalid audit period: 'from' (" + from + ") is after 'to' (" + to + ")");
    }
}
