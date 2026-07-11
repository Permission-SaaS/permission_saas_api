package com.saas.permissions.identity.domain.exception;

import com.saas.permissions.shared.domain.exception.BusinessRuleException;

public class EmailAlreadyInUseException extends BusinessRuleException {
    public EmailAlreadyInUseException(String email) {
        super("Email already in use: " + email);
    }
}
